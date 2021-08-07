package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.gameitem.ItemID

/**
 * Created by minjaesong on 2016-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory
    val actorValue: ActorValue

    /**
     * Equips an item. If the item is not in the inventory, an error will be thrown.
     */
    fun unequipItem(item: GameItem?) {
        if (item == null) return

        if (item.equipPosition == GameItem.EquipPosition.NULL)
            throw Error("Unequipping the item that cannot be equipped in the first place")

        if (!inventory.contains(item)) {
            //throw Error("Unequipping the item that does not exist in inventory")
            System.err.println("[Pocketed] Warning -- Unequipping the item that does not exist in inventory")
            return // just do nothing
        }

        inventory.itemEquipped[item.equipPosition] = null

        // NOTE: DON'T TOUCH QUICKSLOT HERE
        // Relevant Actorvalue is NOT being updated on time
        // They're being safely handled by UIItemInventoryElem*.touchDown() and ActorInventory.remove

        item.effectOnUnequip(AppLoader.UPDATE_RATE)
    }

    fun unequipItem(itemID: ItemID?) {
        itemID?.let {
            unequipItem(ItemCodex[itemID])
        } ?: return
    }

    // no need for equipSlot(Int)
    fun unequipSlot(slot: Int) {
        if (slot < 0 || slot > GameItem.EquipPosition.INDEX_MAX)
            throw IllegalArgumentException("Slot index out of range: $slot")

        unequipItem(inventory.itemEquipped[slot])
    }

    /**
     * Equips an item. If the item is not in the inventory, adds the item first.
     */
    fun equipItem(item: GameItem) {
        if (!inventory.contains(item)) {
            println("[Pocketed] Item does not exist; adding one before equipped")
            inventory.add(item)
        }

        // unequip item that's already there
        unequipSlot(item.equipPosition)

        if (item.equipPosition >= 0) {
            inventory.itemEquipped[item.equipPosition] = item.dynamicID
            item.effectWhenEquipped(AppLoader.UPDATE_RATE)
        }
        // else do nothing
    }

    fun equipItem(itemID: ItemID) {
        equipItem(ItemCodex[itemID]!!)
    }

    fun equipped(item: GameItem): Boolean {
        return inventory.itemEquipped[item.equipPosition] == item.dynamicID
    }
    fun equipped(itemID: ItemID) = equipped(ItemCodex[itemID]!!)

    fun addItem(itemID: ItemID, count: Int = 1) = inventory.add(ItemCodex[itemID]!!, count)
    fun addItem(item: GameItem, count: Int = 1) = inventory.add(item, count)
    fun removeItem(itemID: ItemID, count: Int = 1) = inventory.remove(ItemCodex[itemID]!!, count)
    fun removeItem(item: GameItem, count: Int = 1) = inventory.remove(item, count)

    fun hasItem(item: GameItem) = inventory.contains(item.dynamicID)
    fun hasItem(id: ItemID) = inventory.contains(id)

}