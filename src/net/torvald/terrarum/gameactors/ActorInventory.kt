package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 16-03-15.
 */

class ActorInventory() {

    @Transient val CAPACITY_MAX = 0x7FFFFFFF
    @Transient val CAPACITY_MODE_NO_ENCUMBER = 0
    @Transient val CAPACITY_MODE_COUNT = 1
    @Transient val CAPACITY_MODE_WEIGHT = 2


    private var capacityByCount: Int
    private var capacityByWeight: Int
    private var capacityMode: Int

    /**
     * Sorted by referenceID.
     */
    private val itemList = ArrayList<InventoryPair>()

    /**
     * Default constructor with no encumbrance.
     */
    init {
        capacityMode = CAPACITY_MODE_NO_ENCUMBER
        capacityByCount = 0
        capacityByWeight = 0
    }

    /**
     * Construct new inventory with specified capacity.
     * @param capacity if is_weight is true, killogramme value is required, counts of items otherwise.
     * *
     * @param is_weight whether encumbrance should be calculated upon the weight of the inventory. False to use item counts.
     */
    constructor(capacity: Int, is_weight: Boolean) : this() {
        if (is_weight) {
            capacityByWeight = capacity
            capacityMode = CAPACITY_MODE_WEIGHT
        } else {
            capacityByCount = capacity
            capacityMode = CAPACITY_MODE_COUNT
        }
    }

    fun add(itemID: Int, count: Int = 1) = add(ItemCodex[itemID], count)
    fun add(item: InventoryItem, count: Int = 1) {
        if (item.id == Player.PLAYER_REF_ID)
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (Terrarum.ingame != null &&
            Terrarum.ingame!!.playableActorDelegate != null &&
            item.id == Terrarum.ingame!!.player.referenceID)
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
                add(item, -count)
            }
            else {
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
    fun getCapacity(): Int {
        if (capacityMode == CAPACITY_MODE_NO_ENCUMBER) {
            return CAPACITY_MAX
        }
        else if (capacityMode == CAPACITY_MODE_WEIGHT) {
            return capacityByWeight
        }
        else {
            return capacityByCount
        }
    }

    fun getCapacityMode(): Int {
        return capacityMode
    }

    fun getTotalWeight(): Double {
        var weight = 0.0
        itemList.forEach { weight += it.item.mass * it.amount }

        return weight
    }

    /**
     * Real amount
     */
    fun getTotalCount(): Int {
        var count = 0
        itemList.forEach { count += it.amount }

        return count
    }

    /**
     * Unique amount, multiple items are calculated as one
     */
    fun getTotalUniqueCount(): Int {
        return itemList.size
    }

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    fun isEncumbered(): Boolean {
        if (getCapacityMode() == CAPACITY_MODE_WEIGHT) {
            return capacityByWeight < getTotalWeight()
        } else if (getCapacityMode() == CAPACITY_MODE_COUNT) {
            return capacityByCount < getTotalCount()
        } else {
            return false
        }
    }






    fun hasItem(item: InventoryItem) = hasItem(item.id)
    fun hasItem(id: Int) =
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