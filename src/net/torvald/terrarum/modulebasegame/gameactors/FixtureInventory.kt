package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isBlock
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.util.SortedArrayList
import java.math.BigInteger

/**
 * Created by minjaesong on 2021-03-16.
 */

open class FixtureInventory() {

    open var maxCapacity = 100L
    open var capacityMode = CAPACITY_MODE_COUNT

    constructor(maxCapacity: Long, capacityMode: Int) : this() {
        this.maxCapacity = maxCapacity
        this.capacityMode = capacityMode
    }

    companion object {
        val CAPACITY_MODE_NO_ENCUMBER = 0
        val CAPACITY_MODE_COUNT = 1
        val CAPACITY_MODE_WEIGHT = 2


        private fun filterItem0(item: GameItem): GameItem {
            return if (item.originalID.isBlock()) {
                val drop = BlockCodex[item.originalID].drop.ifBlank { item.originalID }
                ItemCodex[drop] ?: throw NullPointerException("Item: ${item.originalID}, drop: ${BlockCodex[item.originalID].drop}")
            }
            else if (item.originalID.isWall()) {
                val blockID = item.originalID.substringAfter('@')
                val drop = BlockCodex[blockID].drop.ifBlank { blockID }
                ItemCodex["wall@$drop"] ?: throw NullPointerException("Item: ${item.originalID}, drop: ${BlockCodex[item.originalID].drop}")
            }
            else {
                item
            }
        }

        fun filterItem(item0: GameItem?): GameItem? {
            if (item0 == null) return null

            var item = item0
            var recursionCount = 0
            var breakNow = false
            var item1: GameItem
            while (!breakNow) {
                if (recursionCount > 16) throw IllegalStateException("Item filter recursion is too deep, check the filtering code")

                item1 = filterItem0(item!!)

                if (item1 == item) {
                    breakNow = true
                }

                item = item1

                recursionCount++
            }
            return item
        }
    }
    
    /**
     * Sorted by referenceID.
     */
    protected val itemList = SortedArrayList<InventoryPair>()
    var wallet = BigInteger("0") // unified currency for whole civs; Dwarf Fortress approach seems too complicated

    fun isEmpty() = totalCount == 0L
    fun isNotEmpty() = totalCount > 0

    open fun add(itemID: ItemID, count: Long = 1) {
        if (ItemCodex[itemID] == null)
            throw NullPointerException("Item not found: $itemID")
        else
            add(ItemCodex[itemID]!!, count)
    }
    open fun add(item0: GameItem, count: Long = 1L) {

        val item = filterItem(item0)!!

//        printdbg(this, "add-by-elem $item, $count")

        // other invalid values
        if (count == 0L)
//            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Item count is zero.")
            return
        if (count < 0L)
            throw IllegalArgumentException("Item count is negative number. If you intended removing items, use remove()\n" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")
        if (item.originalID == "actor:${Terrarum.PLAYER_REF_ID}" || item.originalID == ("actor:${0x51621D}")) // do not delete this magic
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to put human player into the inventory.")
        if (((Terrarum.ingame as? TerrarumIngame)?.gameFullyLoaded == true) &&
            (item.originalID == "actor:${(Terrarum.ingame as? TerrarumIngame)?.actorNowPlaying?.referenceID}"))
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to put active player into the inventory.")
        if ((!item.stackable || item.dynamicID.startsWith("dyn:")) && count > 1)
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to adding stack of item but the item is not stackable; item: $item, count: $count")



        // If we already have the item, increment the amount
        // If not, add item with specified amount
        val existingItem = searchByID(item.dynamicID)

        // if the item already exists
        if (existingItem != null) {
            // increment count
            if (existingItem.qty + count < 0L) // check numeric overflow
                existingItem.qty = Long.MAX_VALUE
            else
                existingItem.qty += count
        }
        // new item
        else {
            itemList.add(InventoryPair(item.dynamicID, count))
        }
//        insertionSortLastElem(itemList)

        updateEncumbrance()
    }

    open fun removeEntryForced(itemID: ItemID) {
        searchByID(itemID)?.let {
            itemList.remove(it)
        }
    }

    open fun remove(itemID: ItemID, count: Long) = remove(ItemCodex[itemID]!!, count) {}
    open fun remove(item: GameItem, count: Long = 1L) = remove(item, count) {}

    open fun remove(itemID: ItemID, count: Long, unequipFun: (InventoryPair) -> Unit) =
            remove(ItemCodex[itemID]!!, count, unequipFun)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation
     *
     * @return the actual amount of item that has been removed
     **/
    open fun remove(item: GameItem, count: Long = 1, unequipFun: (InventoryPair) -> Unit): Long {

//        printdbg(this, "remove $item, $count")

        if (count == 0L)
//            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Item count is zero.")
            return 0L
        if (count < 0L)
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Item count is negative number. If you intended adding items, use add()" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")


        var delta = 0L

        val existingItem = searchByID(item.dynamicID)
        if (existingItem != null) { // if the item already exists
            val newCount = existingItem.qty - count

            /*if (newCount < 0) {
                throw InventoryFailedTransactionError("[${this.javaClass.canonicalName}] Tried to remove $count of $item, but the inventory only contains ${existingItem.qty} of them.")
            }
            else*/ if (newCount > 0) {
                delta = count
                // decrement count
                existingItem.qty = newCount
            }
            else {
                delta = existingItem.qty
                // unequip must be done before the entry removal
                unequipFun(existingItem)
                // depleted item; remove entry from inventory
                itemList.remove(existingItem)
            }
        }
        else {
//            throw InventoryFailedTransactionError("[${this.javaClass.canonicalName}] Tried to remove $item, but the inventory does not have it.")
            return 0L
        }

//        itemList.sumOf { ItemCodex[it.itm]!!.mass * it.qty } // ???

        updateEncumbrance()

        return delta
    }

    open fun clear(): List<InventoryPair> {
        val r = itemList.cloneToList()
        itemList.clear()
        return r
    }
    
    /**
     * HashMap<GameItem, Amounts>
     */
    fun forEach(consumer: (InventoryPair) -> Unit) {
        itemList.forEach(consumer)
        updateEncumbrance()
    }

    fun first(predicate: (InventoryPair) -> Boolean) = itemList.first(predicate)
    fun all(predicate: (InventoryPair) -> Boolean) = itemList.all(predicate)
    fun any(predicate: (InventoryPair) -> Boolean) = itemList.any(predicate)
    fun none(predicate: (InventoryPair) -> Boolean) = itemList.none()
    fun filter(predicate: (InventoryPair) -> Boolean) = itemList.filter(predicate)
    fun map(transformation: (InventoryPair) -> Any) = itemList.map(transformation)

    /**
     * Get capacity of inventory
     * @return
     */
    val capacity: Double
        get() = if (capacityMode == CAPACITY_MODE_NO_ENCUMBER)
            maxCapacity.toDouble()
        else if (capacityMode == CAPACITY_MODE_WEIGHT)
            totalWeight
        else
            totalCount.toDouble()

    @Transient private var totalWeight0 = -1.0

    val totalWeight: Double// = itemList.sumOf { ItemCodex[it.itm]!!.mass * it.qty }
        get() {
            if (totalWeight0 < 0.0) updateEncumbrance()
            return totalWeight0
        }

    @Transient private var totalCount0 = -1L

    /**
     * Real amount
     */
    val totalCount: Long
        get() {
            if (totalCount0 < 0) updateEncumbrance()
            return totalCount0
        }

    /**
     * Unique amount, multiple items are calculated as one
     */
    val totalUniqueCount: Long
        get() = itemList.size.toLong()

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    val isEncumbered: Boolean
        get() = encumberment >= 1.0

    /**
     * How encumbered the actor is. 1.0 if weight of the items are exactly same as the capacity limit, >1.0 if encumbered.
     */
    open val encumberment: Double
        get() = if (capacityMode == CAPACITY_MODE_NO_ENCUMBER)
            0.0
        else if (capacityMode == CAPACITY_MODE_WEIGHT)
            capacity / maxCapacity
        else
            0.0
    
    fun contains(item: GameItem) = contains(item.dynamicID)
    fun contains(id: ItemID) =
            if (itemList.size == 0)
                false
            else
                itemList.contains(InventoryPair(id, 1))
    fun searchByID(id: ItemID?): InventoryPair? {
        if (itemList.size == 0 || id == null)
            return null

        return itemList.searchFor(id) { it.itm }
    }
    /*protected fun insertionSortLastElem(arr: ArrayList<InventoryPair>) {
        ReentrantLock().lock {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j].itm > x.itm) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }*/
    @Transient private val STATIC_ID = 41324534
    @Transient private val DYNAMIC_ID = 181643953
    protected fun ArrayList<InventoryPair>.binarySearch(ID: ItemID, searchMode: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = if (searchMode == STATIC_ID)
                ItemCodex[this[mid].itm]!!.originalID
            else
                ItemCodex[this[mid].itm]!!.dynamicID

            if (ID > midVal)
                low = mid + 1
            else if (ID < midVal)
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }

    fun updateEncumbrance() {
        totalWeight0 = itemList.sumOf { ItemCodex[it.itm]!!.mass * it.qty }
        totalCount0 = itemList.sumOf { it.qty }
    }
}

class InventoryPair : Comparable<InventoryPair> {

    var itm: ItemID = ""; private set
    var qty: Long = 0

    private constructor()

    constructor(item: ItemID, quantity: Long) : this() {
        itm = item
        qty = quantity
    }

    operator fun component1() = itm
    operator fun component2() = qty

    override fun compareTo(other: InventoryPair) = this.itm.compareTo(other.itm)

    override fun toString(): String {
        return if (qty == -1L) "$itm" else "$itm ($qty)"
    }

    fun set(itm: ItemID, qty: Long) {
        this.itm = itm
        this.qty = qty
    }
}

class InventoryTransactionFailedError(msg: String) : Error(msg)