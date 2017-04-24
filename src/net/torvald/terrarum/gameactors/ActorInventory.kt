package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UIInventory
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 16-03-15.
 */

class ActorInventory(val actor: Pocketed, var maxCapacity: Int, var capacityMode: Int) {

    companion object {
        @Transient val CAPACITY_MODE_NO_ENCUMBER = 0
        @Transient val CAPACITY_MODE_COUNT = 1
        @Transient val CAPACITY_MODE_WEIGHT = 2
    }

    /**
     * List of all equipped items (tools, armours, rings, necklaces, etc.)
     */
    val itemEquipped = Array<InventoryItem?>(InventoryItem.EquipPosition.INDEX_MAX, { null })

    /**
     * Sorted by referenceID.
     */
    private val itemList = ArrayList<InventoryPair>()

    init {
    }

    fun add(itemID: Int, count: Int = 1) = add(ItemCodex[itemID], count)
    fun add(item: InventoryItem, count: Int = 1) {

        if (item.dynamicID == Player.PLAYER_REF_ID || item.dynamicID == 0x51621D) // do not delete this magic
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (Terrarum.ingame != null &&
            (item.dynamicID == Terrarum.ingame?.player?.referenceID))
            throw IllegalArgumentException("Attempted to put active player into the inventory.")



        // If we already have the item, increment the amount
        // If not, add item with specified amount
        val existingItem = getByDynamicID(item.dynamicID)

        // if the item already exists
        if (existingItem != null) {
            val newCount = getByDynamicID(item.dynamicID)!!.amount + count
            itemList.remove(existingItem)
            itemList.add(InventoryPair(existingItem.item, newCount))
        }
        // new item
        else {
            if (item.isDynamic) {
                // assign new ID for each
                repeat(count) {
                    val newItem = item.clone().generateUniqueDynamicID(this)
                    itemList.add(InventoryPair(newItem, 1))
                }
            }
            else {
                itemList.add(InventoryPair(item, count))
            }
        }
        insertionSortLastElem(itemList)
    }

    fun remove(itemID: Int, count: Int) = remove(ItemCodex[itemID], count)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation */
    fun remove(item: InventoryItem, count: Int = 1) {
        val existingItem = getByDynamicID(item.dynamicID)
        if (existingItem != null) { // if the item already exists
            val newCount = getByDynamicID(item.dynamicID)!!.amount - count
            if (newCount < 0) {
                throw Error("Tried to remove $count of $item, but the inventory only contains ${getByDynamicID(item.dynamicID)!!.amount} of them.")
            }
            else if (newCount > 0) {
                // decrement count
                add(item, -count)
            }
            else {
                // unequip, if applicable
                actor.unequipItem(existingItem.item)
                // depleted item; remove entry from inventory
                itemList.remove(existingItem)
            }
        }
        else {
            throw Error("Tried to remove $item, but the inventory does not have it.")
        }
    }

    /**
     * HashMap<InventoryItem, Amounts>
     */
    fun forEach(consumer: (InventoryPair) -> Unit) = itemList.forEach(consumer)

    /**
     * Get capacity of inventory
     * @return
     */
    val capacity: Double
        get() = if (capacityMode == CAPACITY_MODE_NO_ENCUMBER)
            maxCapacity.toDouble()
        else if (capacityMode == CAPACITY_MODE_WEIGHT)
            getTotalWeight()
        else
            getTotalCount().toDouble()

    fun getTotalWeight(): Double = itemList.map { it.item.mass * it.amount }.sum()

    /**
     * Real amount
     */
    fun getTotalCount(): Int = itemList.map { it.amount }.sum()

    /**
     * Unique amount, multiple items are calculated as one
     */
    fun getTotalUniqueCount(): Int = itemList.size

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    val isEncumbered: Boolean
        get() = if (capacityMode == CAPACITY_MODE_NO_ENCUMBER)
            false
        else if (capacityMode == CAPACITY_MODE_WEIGHT)
            maxCapacity < capacity
        else
            false


    fun consumeItem(actor: Actor, item: InventoryItem) {
        if (item.consumable) {
            remove(item, 1)
        }
        else {
            // unpack newly-made dynamic item (e.g. any weapon, floppy disk)
            /*if (item.isDynamic && item.originalID == item.dynamicID) {
                remove(item.originalID, 1)
                item.generateUniqueDynamicID(this)
                add(item)
            }*/



            // calculate damage value
            val baseDamagePerSwing = if (actor is ActorHumanoid)
                actor.avStrength / 1000.0
            else
                1.0 // TODO variable: scale, strength
            val swingDmgToFrameDmg = Terrarum.delta.toDouble() / actor.actorValue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            // damage the item
            item.durability -= (baseDamagePerSwing * swingDmgToFrameDmg).toFloat()
            if (item.durability <= 0)
                remove(item, 1)

            println("[ActorInventory] consumed; ${item.durability}")
        }
    }







    fun contains(item: InventoryItem) = contains(item.dynamicID)
    fun contains(id: Int) =
            if (itemList.size == 0)
                false
            else
                itemList.binarySearch(id) >= 0
    fun getByDynamicID(id: Int): InventoryPair? {
        if (itemList.size == 0)
            return null

        val index = itemList.binarySearch(id)
        if (index < 0)
            return null
        else
            return itemList[index]
    }
    private fun insertionSortLastElem(arr: ArrayList<InventoryPair>) {
        lock(ReentrantLock()) {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j].item > x.item) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }
    private fun ArrayList<InventoryPair>.binarySearch(ID: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = get(mid).item.dynamicID

            if (ID > midVal)
                low = mid + 1
            else if (ID < midVal)
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }
    inline fun lock(lock: Lock, body: () -> Unit) {
        lock.lock()
        try {
            body()
        }
        finally {
            lock.unlock()
        }
    }
}

data class InventoryPair(val item: InventoryItem, val amount: Int)