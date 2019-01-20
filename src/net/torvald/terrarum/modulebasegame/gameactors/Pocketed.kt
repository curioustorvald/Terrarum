package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 2016-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory

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
        item.effectOnUnequip(AppLoader.getSmoothDelta().toFloat())
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

        if (item.equipPosition >= 0) {
            inventory.itemEquipped[item.equipPosition] = item
            item.effectWhenEquipped(AppLoader.getSmoothDelta().toFloat())
        }
        // else do nothing
    }

    fun equipped(item: GameItem): Boolean {
        return inventory.itemEquipped[item.equipPosition] == item
    }

    fun addItem(itemID: Int, count: Int = 1) = inventory.add(ItemCodex[itemID], count)
    fun addItem(item: GameItem, count: Int = 1) = inventory.add(item, count)
    fun removeItem(itemID: Int, count: Int = 1) = inventory.remove(ItemCodex[itemID], count)
    fun removeItem(item: GameItem, count: Int = 1) = inventory.remove(item, count)

    fun hasItem(item: GameItem) = inventory.contains(item.dynamicID)
    fun hasItem(id: Int) = inventory.contains(id)


    fun consumePrimary(item: GameItem) {
        if (item.startPrimaryUse(AppLoader.getSmoothDelta().toFloat())) {
            inventory.consumeItem(this as Actor, item) // consume on successful
        }
    }

    fun consumeSecondary(item: GameItem) {
        if (item.startSecondaryUse(AppLoader.getSmoothDelta().toFloat()))
            inventory.consumeItem(this as Actor, item) // consume on successful
    }
}