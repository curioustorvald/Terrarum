package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.InventoryItem
import java.util.*

/**
 * Created by minjaesong on 16-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory


    fun unequipItem(item: InventoryItem) {
        if (item.equipPosition == InventoryItem.EquipPosition.NULL)
            throw Error("Unequipping the item that cannot be equipped")

        if (!inventory.hasItem(item))
            throw Error("Unequipping the item that does not exist in inventory")

        inventory.itemEquipped[item.equipPosition] = null
        item.effectOnUnequip(Terrarum.appgc, Terrarum.UPDATE_DELTA)
    }

    fun equipItem(item: InventoryItem) {
        if (item.equipPosition >= 0) {
            inventory.itemEquipped[item.equipPosition] = item
            item.effectWhenEquipped(Terrarum.appgc, Terrarum.UPDATE_DELTA)
        }
    }

    fun isEquipped(item: InventoryItem): Boolean {
        return inventory.itemEquipped[item.equipPosition] == item
    }



    fun consumePrimary(item: InventoryItem) {
        if (item.primaryUse(Terrarum.appgc, Terrarum.UPDATE_DELTA))
            inventory.consumeItem(item) // consume on successful
    }

    fun consumeSecondary(item: InventoryItem) {
        if (item.secondaryUse(Terrarum.appgc, Terrarum.UPDATE_DELTA))
            inventory.consumeItem(item) // consume on successful
    }
}