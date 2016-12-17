package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
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
    private val itemList: HashMap<InventoryItem, Int> = HashMap()

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
        if (Terrarum.ingame.playableActorDelegate != null &&
                item.id == Terrarum.ingame.player.referenceID)
            throw IllegalArgumentException("Attempted to put active player into the inventory.")

        // If we already have the item, increment the amount
        // If not, add item with specified amount
        itemList.put(item, itemList[item] ?: 0 + count)
    }

    fun remove(itemID: Int, count: Int = 1) = remove(ItemCodex[itemID], count)
    fun remove(item: InventoryItem, count: Int = 1) {
        // check if the item does NOT exist
        if (itemList[item] == null) {
            return
        }
        else {
            // remove the existence of the item if count <= 0
            if (itemList[item]!! - count <= 0) {
                itemList.remove(item)
            }
            // else, decrement the item count
            else {
                itemList.put(item, itemList[item]!! - count)
            }
        }
    }


    fun contains(item: InventoryItem) = itemList.containsKey(item)
    fun contains(itemID: Int) = itemList.containsKey(ItemCodex[itemID])

    fun forEach(consumer: (InventoryItem, Int) -> Unit) = itemList.forEach(consumer)

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
    fun getItemList(): Map<InventoryItem, Int>? {
        return itemList
    }

    /**
     * Get clone of the itemList
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    fun getCopyOfItemList(): Map<InventoryItem, Int>? {
        return itemList.clone() as Map<InventoryItem, Int>
    }

    fun getTotalWeight(): Double {
        var weight = 0.0
        itemList.forEach { item, i -> weight += item.mass * i }

        return weight
    }

    /**
     * Real amount
     */
    fun getTotalCount(): Int {
        var count = 0
        itemList.forEach { item, i -> count += i }

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
            return capacityByCount < getTotalWeight()
        } else {
            return false
        }
    }

}