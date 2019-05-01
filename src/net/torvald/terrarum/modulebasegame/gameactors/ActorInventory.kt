package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_DYNAMIC
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_WALLS
import net.torvald.terrarum.itemproperties.ItemID
import net.torvald.terrarum.lock
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.ui.UIQuickslotBar
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 2016-03-15.
 */

class ActorInventory(@Transient val actor: Pocketed, var maxCapacity: Int, var capacityMode: Int) {

    companion object {
        val CAPACITY_MODE_NO_ENCUMBER = 0
        val CAPACITY_MODE_COUNT = 1
        val CAPACITY_MODE_WEIGHT = 2
    }

    // FIXME unless absolutely necessary, don't store full item object; only store its dynamicID

    /**
     * List of all equipped items (tools, armours, rings, necklaces, etc.)
     */
    val itemEquipped = Array<ItemID?>(GameItem.EquipPosition.INDEX_MAX) { null }

    /**
     * Sorted by referenceID.
     */
    val itemList = ArrayList<InventoryPair>()
    val quickSlot = Array<ItemID?>(UIQuickslotBar.SLOT_COUNT) { null } // 0: Slot 1, 9: Slot 10

    var wallet = 0 // unified currency for whole civs; Dwarf Fortress approach seems too complicated

    init {
    }

    fun add(itemID: ItemID, count: Int = 1) = add(ItemCodex[itemID]!!, count)
    fun add(item: GameItem, count: Int = 1) {

        println("[ActorInventory] add $item, $count")


        // not wall-able walls
        if (item.inventoryCategory == GameItem.Category.WALL &&
            !BlockCodex[item.dynamicID - ITEM_WALLS.start].isWallable) {
            throw IllegalArgumentException("Wall ID ${item.dynamicID - ITEM_WALLS.start} is not wall-able.")
        }


        // other invalid values
        if (count == 0)
            throw IllegalArgumentException("Item count is zero.")
        if (count < 0)
            throw IllegalArgumentException("Item count is negative number. If you intended removing items, use remove()\n" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")
        if (item.originalID == Terrarum.PLAYER_REF_ID || item.originalID == 0x51621D) // do not delete this magic
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (((Terrarum.ingame as? Ingame)?.gameFullyLoaded ?: false) &&
            (item.originalID == (Terrarum.ingame as? Ingame)?.actorNowPlaying?.referenceID))
            throw IllegalArgumentException("Attempted to put active player into the inventory.")
        if ((!item.stackable || item.dynamicID in ITEM_DYNAMIC) && count > 1)
            throw IllegalArgumentException("Attempting to adding stack of item but the item is not stackable; item: $item, count: $count")



        // If we already have the item, increment the amount
        // If not, add item with specified amount
        val existingItem = invSearchByDynamicID(item.dynamicID)

        // if the item already exists
        if (existingItem != null) {
            // increment count
            existingItem.amount += count
        }
        // new item
        else {
            itemList.add(InventoryPair(item.dynamicID, count))
        }
        insertionSortLastElem(itemList)
    }

    fun remove(itemID: ItemID, count: Int) = remove(ItemCodex[itemID]!!, count)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation */
    fun remove(item: GameItem, count: Int = 1) {

        println("[ActorInventory] remove $item, $count")

        if (count == 0)
            throw IllegalArgumentException("Item count is zero.")
        if (count < 0)
            throw IllegalArgumentException("Item count is negative number. If you intended adding items, use add()" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")



        val existingItem = invSearchByDynamicID(item.dynamicID)
        if (existingItem != null) { // if the item already exists
            val newCount = existingItem.amount - count

            if (newCount < 0) {
                throw Error("Tried to remove $count of $item, but the inventory only contains ${existingItem.amount} of them.")
            }
            else if (newCount > 0) {
                // decrement count
                existingItem.amount = newCount
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

    fun setQuickBar(slot: Int, dynamicID: ItemID?) {
        quickSlot[slot] = dynamicID
    }

    fun getQuickslot(slot: Int): InventoryPair? = invSearchByDynamicID(quickSlot[slot])

    /**
     * HashMap<GameItem, Amounts>
     */
    inline fun forEach(consumer: (InventoryPair) -> Unit) = itemList.forEach(consumer)

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

    fun getTotalWeight(): Double = itemList.map { ItemCodex[it.item]!!.mass * it.amount }.sum()

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


    fun consumeItem(item: GameItem) {
        val actor = this.actor as Actor

        if (item.stackable && !item.isDynamic) {
            remove(item, 1)
        }
        else if (item.isUnique) {
            return // don't consume a bike!
        }
        else {
            val newItem: GameItem

            // unpack newly-made dynamic item (e.g. any weapon, floppy disk)
            if (item.isDynamic && item.originalID == item.dynamicID) {
                itemEquipped[item.equipPosition] = null
                remove(item, 1)


                newItem = item.clone()
                newItem.generateUniqueDynamicID(this)

                newItem.stackable = false
                add(newItem)
                itemEquipped[newItem.equipPosition] = newItem.dynamicID //invSearchByDynamicID(newItem.dynamicID)!!.item // will test if some sketchy code is written. Test fail: kotlinNullpointerException

                // FIXME now damage meter (vital) is broken
            }
            else {
                newItem = item
            }



            // calculate damage value
            val baseDamagePerSwing = if (actor is ActorHumanoid)
                actor.avStrength / 1000.0
            else
                1.0 // TODO variable: scale, strength
            val swingDmgToFrameDmg = AppLoader.UPDATE_RATE.toFloat().toDouble() / actor.actorValue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            // damage the item
            newItem.durability -= (baseDamagePerSwing * swingDmgToFrameDmg).toFloat()
            if (newItem.durability <= 0)
                remove(newItem, 1)

            //println("[ActorInventory] consumed; ${item.durability}")
        }
    }







    fun contains(item: GameItem) = contains(item.dynamicID)
    fun contains(id: ItemID) =
            if (itemList.size == 0)
                false
            else
                itemList.binarySearch(id, DYNAMIC_ID) >= 0
    fun invSearchByDynamicID(id: ItemID?): InventoryPair? {
        if (itemList.size == 0 || id == null)
            return null

        val index = itemList.binarySearch(id, DYNAMIC_ID)
        if (index < 0)
            return null
        else
            return itemList[index]
    }
    private fun invSearchByStaticID(id: ItemID?): InventoryPair? {
        if (itemList.size == 0 || id == null)
            return null

        val index = itemList.binarySearch(id, STATIC_ID)
        if (index < 0)
            return null
        else
            return itemList[index]
    }
    private fun insertionSortLastElem(arr: ArrayList<InventoryPair>) {
        ReentrantLock().lock {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j].item > x.item) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }
    @Transient private val STATIC_ID = 41324534
    @Transient private val DYNAMIC_ID = 181643953
    private fun ArrayList<InventoryPair>.binarySearch(ID: ItemID, searchMode: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = if (searchMode == STATIC_ID)
                ItemCodex[this[mid].item]!!.originalID
            else
                ItemCodex[this[mid].item]!!.dynamicID

            if (ID > midVal)
                low = mid + 1
            else if (ID < midVal)
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }
}

data class InventoryPair(val item: ItemID, var amount: Int)