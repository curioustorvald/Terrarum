package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.InventoryItem
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
        if (item.id == Player.PLAYER_REF_ID || item.id == 0x51621D) // do not delete this magic
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (Terrarum.ingame != null &&
            (item.id == Terrarum.ingame?.player?.referenceID))
            throw IllegalArgumentException("Attempted to put active player into the inventory.")

        // If we already have the item, increment the amount
        // If not, add item with specified amount
        val existingItem = getByID(item.id)
        if (existingItem != null) { // if the item already exists
            val newCount = getByID(item.id)!!.amount + count
            itemList.remove(existingItem)
            itemList.add(InventoryPair(existingItem.item, newCount))
        }
        else {
            itemList.add(InventoryPair(item, count))
        }
        insertionSortLastElem(itemList)
    }

    fun remove(itemID: Int, count: Int = 1) = remove(ItemCodex[itemID], count)
    fun remove(item: InventoryItem, count: Int = 1) {
        val existingItem = getByID(item.id)
        if (existingItem != null) { // if the item already exists
            val newCount = getByID(item.id)!!.amount - count
            if (newCount < 0) {
                throw Error("Tried to remove $count of $item, but the inventory only contains ${getByID(item.id)!!.amount} of them.")
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


    fun consumeItem(item: InventoryItem) {
        if (item.consumable) {
            remove(item, 1)
        }
        else {
            item.durability -= 1f
            if (item.durability <= 0)
                remove(item, 1)
        }
    }







    fun contains(item: InventoryItem) = contains(item.id)
    fun contains(id: Int) =
            if (itemList.size == 0)
                false
            else
                itemList.binarySearch(id) >= 0
    fun getByID(id: Int): InventoryPair? {
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

            val midVal = get(mid).item

            if (ID > midVal.id)
                low = mid + 1
            else if (ID < midVal.id)
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