package com.Torvald.Terrarum.Actors

import com.Torvald.Terrarum.GameItem.InventoryItem
import com.Torvald.Terrarum.GameItem.ItemCodex
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */

@Transient const val CAPACITY_MAX = 0x7FFFFFFF
@Transient const val CAPACITY_MODE_NO_ENCUMBER = 0
@Transient const val CAPACITY_MODE_COUNT = 1
@Transient const val CAPACITY_MODE_WEIGHT = 2

class ActorInventory {

    private var capacityByCount: Int = 0
    private var capacityByWeight: Int = 0
    private var capacityMode: Int = 0

    /**
     * &lt;ReferenceID, Amounts&gt;
     */
    private val itemList: HashMap<Long, Int> = HashMap()

    /**
     * Construct new inventory with specified capacity.
     * @param capacity if is_weight is true, killogramme value is required, counts of items otherwise.
     * *
     * @param is_weight whether encumbrance should be calculated upon the weight of the inventory. False to use item counts.
     */
    constructor(capacity: Int, is_weight: Boolean) {
        if (is_weight) {
            capacityByWeight = capacity
            capacityMode = CAPACITY_MODE_WEIGHT
        } else {
            capacityByCount = capacity
            capacityMode = CAPACITY_MODE_COUNT
        }
    }

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

    /**
     * Get reference to the itemList
     * @return
     */
    fun getItemList(): Map<Long, Int>? {
        return itemList
    }

    /**
     * Get clone of the itemList
     * @return
     */
    fun getCopyOfItemList(): Map<Long, Int>? {
        return itemList.clone() as Map<Long, Int>
    }

    fun getTotalWeight(): Float {
        var weight = 0f

        for (item in itemList.entries) {
            weight += ItemCodex.getItem(item.key).weight * item.value
        }

        return weight
    }

    fun getTotalCount(): Int {
        var count = 0

        for (item in itemList.entries) {
            count += item.value
        }

        return count
    }

    fun getTotalUniqueCount(): Int {
        return itemList.entries.size
    }

    fun appendToPocket(item: InventoryItem) {
        appendToPocket(item, 1)
    }

    fun appendToPocket(item: InventoryItem, count: Int) {
        val key = item.itemID

        // if (key == Player.PLAYER_REF_ID)
        //     throw new IllegalArgumentException("Attempted to put player into the inventory.");

        if (itemList.containsKey(key))
        // increment amount if it already has specified item
            itemList.put(key, itemList[key]!! + count)
        else
        // add new entry if it does not have specified item
            itemList.put(key, count)
    }

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    fun isEncumbered(): Boolean {
        if (getCapacityMode() == CAPACITY_MODE_WEIGHT) {
            return capacityByWeight < getTotalWeight()
        } else if (getCapacityMode() == CAPACITY_MODE_COUNT) {
            return capacityByCount < getTotalWeight()
        } else {
            return false
        }
    }

}