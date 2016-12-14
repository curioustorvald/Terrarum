package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
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
     * HashMap<ReferenceID, Amounts>
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

    fun add(item: InventoryItem, count: Int = 1) = add(item.itemID, count)
    fun add(itemID: Int, count: Int = 1) {
        if (itemID == Player.PLAYER_REF_ID)
            throw IllegalArgumentException("Attempted to put human player into the inventory.")
        if (Terrarum.ingame.playableActorDelegate != null &&
                itemID == Terrarum.ingame.player.referenceID)
            throw IllegalArgumentException("Attempted to put active player into the inventory.")

        // If we already have the item, increment the amount
        // If not, add item with specified amount
        itemList.put(itemID, itemList[itemID] ?: 0 + count)
    }

    fun remove(item: InventoryItem, count: Int = 1) = remove(item.itemID, count)
    fun remove(itemID: Int, count: Int = 1) {
        // check if the item does NOT exist
        if (itemList[itemID] == null) {
            return
        }
        else {
            // remove the existence of the item if count <= 0
            if (itemList[itemID]!! - count <= 0) {
                itemList.remove(itemID)
            }
            // else, decrement the item count
            else {
                itemList.put(itemID, itemList[itemID]!! - count)
            }
        }
    }


    fun contains(item: InventoryItem) = itemList.containsKey(item.itemID)
    fun contains(itemID: Int) = itemList.containsKey(itemID)

    fun forEach(consumer: (Int, Int) -> Unit) = itemList.forEach(consumer)

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

    fun getTotalWeight(): Double {
        var weight = 0.0

        for (item in itemList.entries) {
            weight += ItemPropCodex.getProp(item.key).mass * item.value
        }

        return weight
    }

    /**
     * Real amount
     */
    fun getTotalCount(): Int {
        var count = 0

        for (item in itemList.entries) {
            count += item.value
        }

        return count
    }

    /**
     * Unique amount, multiple items are calculated as one
     */
    fun getTotalUniqueCount(): Int {
        return itemList.entries.size
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