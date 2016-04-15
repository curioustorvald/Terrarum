package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemPropCodex
import java.util.*

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
     * &lt;ReferenceID, Amounts&gt;
     */
    private val itemList: HashMap<Int, Int> = HashMap()

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
    fun getItemList(): Map<Int, Int>? {
        return itemList
    }

    /**
     * Get clone of the itemList
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    fun getCopyOfItemList(): Map<Int, Int>? {
        return itemList.clone() as Map<Int, Int>
    }

    fun getTotalWeight(): Float {
        var weight = 0f

        for (item in itemList.entries) {
            weight += ItemPropCodex.getItem(item.key).mass * item.value
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