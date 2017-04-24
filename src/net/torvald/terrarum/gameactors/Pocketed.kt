package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 16-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory

    /**
     * Equips an item. If the item is not in the inventory, an error will be thrown.
     */
    fun unequipItem(item: InventoryItem) {
        if (item.equipPosition == InventoryItem.EquipPosition.NULL)
            throw Error("Unequipping the item that cannot be equipped in the first place")

        if (!inventory.contains(item)) {
            //throw Error("Unequipping the item that does not exist in inventory")
            System.err.println("[Pocketed] Warning -- Unequipping the item that does not exist in inventory")
            return // just do nothing
        }

        inventory.itemEquipped[item.equipPosition] = null
        item.effectOnUnequip(Terrarum.appgc, Terrarum.delta)
    }

    /**
     * Equips an item. If the item is not in the inventory, adds the item first.
     */
    fun equipItem(item: InventoryItem) {
        if (!inventory.contains(item)) {
            println("[Pocketed] Item does not exist; adding one before equipped")
            inventory.add(item)
        }

        if (item.equipPosition >= 0) {
            inventory.itemEquipped[item.equipPosition] = item
            item.effectWhenEquipped(Terrarum.appgc, Terrarum.delta)
        }
        // else do nothing
    }

    fun equipped(item: InventoryItem): Boolean {
        return inventory.itemEquipped[item.equipPosition] == item
    }

    fun addItem(itemID: Int, count: Int = 1) = inventory.add(ItemCodex[itemID], count)
    fun addItem(item: InventoryItem, count: Int = 1) = inventory.add(item, count)
    fun removeItem(itemID: Int, count: Int = 1) = inventory.remove(ItemCodex[itemID], count)
    fun removeItem(item: InventoryItem, count: Int = 1) = inventory.remove(item, count)

    fun hasItem(item: InventoryItem) = inventory.contains(item.dynamicID)
    fun hasItem(id: Int) = inventory.contains(id)


    fun consumePrimary(item: InventoryItem) {
        if (item.primaryUse(Terrarum.appgc, Terrarum.delta)) {
            inventory.consumeItem(this as Actor, item) // consume on successful
        }
    }

    fun consumeSecondary(item: InventoryItem) {
        if (item.secondaryUse(Terrarum.appgc, Terrarum.delta))
            inventory.consumeItem(this as Actor, item) // consume on successful
    }
}