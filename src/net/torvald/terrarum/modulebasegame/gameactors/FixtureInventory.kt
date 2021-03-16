package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.lock
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import java.math.BigInteger
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 2021-03-16.
 */

open class FixtureInventory(var maxCapacity: Int, var capacityMode: Int) {
    
    companion object {
        val CAPACITY_MODE_NO_ENCUMBER = 0
        val CAPACITY_MODE_COUNT = 1
        val CAPACITY_MODE_WEIGHT = 2
    }
    
    /**
     * Sorted by referenceID.
     */
    val itemList = ArrayList<InventoryPair>()
    var wallet = BigInteger("0") // unified currency for whole civs; Dwarf Fortress approach seems too complicated
    
    open fun add(itemID: ItemID, count: Int = 1) {
        if (ItemCodex[itemID] == null)
            throw NullPointerException("Item not found: $itemID")
        else
            add(ItemCodex[itemID]!!, count)
    }
    open fun add(item: GameItem, count: Int = 1) {

        println("[ActorInventory] add-by-elem $item, $count")

        // other invalid values
        if (count == 0)
            throw IllegalArgumentException("Item count is zero.")
        if (count < 0)
            throw IllegalArgumentException("Item count is negative number. If you intended removing items, use remove()\n" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")
        if (item.originalID == "actor:${Terrarum.PLAYER_REF_ID}" || item.originalID == ("actor:${0x51621D}")) // do not delete this magic
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (((Terrarum.ingame as? TerrarumIngame)?.gameFullyLoaded ?: false) &&
            (item.originalID == "actor:${(Terrarum.ingame as? TerrarumIngame)?.actorNowPlaying?.referenceID}"))
            throw IllegalArgumentException("Attempted to put active player into the inventory.")
        if ((!item.stackable || item.dynamicID.startsWith("dyn:")) && count > 1)
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

    open fun remove(itemID: ItemID, count: Int) = remove(ItemCodex[itemID]!!, count) {}
    open fun remove(item: GameItem, count: Int = 1) = remove(item, count) {}

    open fun remove(itemID: ItemID, count: Int, unequipFun: (InventoryPair) -> Unit) =
            remove(ItemCodex[itemID]!!, count, unequipFun)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation */
    open fun remove(item: GameItem, count: Int = 1, unequipFun: (InventoryPair) -> Unit) {

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
                // depleted item; remove entry from inventory
                itemList.remove(existingItem)
                // do additional removal job (e.g. unequipping)
                unequipFun(existingItem)
            }
        }
        else {
            throw Error("Tried to remove $item, but the inventory does not have it.")
        }
    }
    
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
    protected fun invSearchByStaticID(id: ItemID?): InventoryPair? {
        if (itemList.size == 0 || id == null)
            return null

        val index = itemList.binarySearch(id, STATIC_ID)
        if (index < 0)
            return null
        else
            return itemList[index]
    }
    protected fun insertionSortLastElem(arr: ArrayList<InventoryPair>) {
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
    protected fun ArrayList<InventoryPair>.binarySearch(ID: ItemID, searchMode: Int): Int {
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