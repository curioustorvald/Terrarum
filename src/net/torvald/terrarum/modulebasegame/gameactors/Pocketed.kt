package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.printStackTrace

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

        // check equipposition of the given item
        if (item.equipPosition == GameItem.EquipPosition.NULL)
            throw Error("Unequipping the item that cannot be equipped in the first place")

        // check if the actor even has the items
        if (!inventory.contains(item)) {
            //throw Error("Unequipping the item that does not exist in inventory")
            System.err.println("[Pocketed] Warning -- Unequipping the item that does not exist in inventory")
            return // just do nothing
        }

        // check if the actor actually have the item equipped
        if (inventory.itemEquipped[item.equipPosition] == item.dynamicID) {
            inventory.itemEquipped[item.equipPosition] = null

            // NOTE: DON'T TOUCH QUICKSLOT HERE
            // Relevant Actorvalue is NOT being updated on time
            // They're being safely handled by UIItemInventoryElem*.touchDown() and ActorInventory.remove

            item.effectOnUnequip(this as ActorWithBody)
        }
    }

    fun unequipItem(itemID: ItemID?) {
        unequipItem(ItemCodex[itemID])
    }

    // no need for equipSlot(Int)
    fun unequipSlot(slot: Int) {
        if (slot < 0 || slot > GameItem.EquipPosition.INDEX_MAX)
            throw IllegalArgumentException("Slot index out of range: $slot")

        val itemID = inventory.itemEquipped[slot]

        if (itemID != null) {
            val item = ItemCodex[itemID]!!
            inventory.itemEquipped[slot] = null
            item.effectOnUnequip(this as ActorWithBody)
        }
    }

    /**
     * Equips an item. If the item is not in the inventory, adds the item first.
     */
    fun equipItem(item: GameItem) {
        val oldItemID = inventory.itemEquipped[item.equipPosition]

        if (!inventory.contains(item)) {
            println("[Pocketed] Item does not exist; adding one before equipped")
            inventory.add(item)
        }

        // unequip item that's already there
        if (item.dynamicID != oldItemID) {
            unequipSlot(item.equipPosition) // also fires effectOnUnequip unconditionally
        }

        if (item.equipPosition >= 0) {
            inventory.itemEquipped[item.equipPosition] = item.dynamicID
            item.effectWhileEquipped(this as ActorWithBody, App.UPDATE_RATE)
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

    fun addItem(itemID: ItemID, count: Long = 1L) = inventory.add(ItemCodex[itemID]!!, count)
    fun addItem(item: GameItem, count: Long = 1L) = inventory.add(item, count)
    fun removeItem(itemID: ItemID, count: Long = 1L) = inventory.remove(ItemCodex[itemID]!!, count)
    fun removeItem(item: GameItem, count: Long = 1L) = inventory.remove(item, count)

    fun hasItem(item: GameItem) = inventory.contains(item.dynamicID)
    fun hasItem(id: ItemID) = inventory.contains(id)

}