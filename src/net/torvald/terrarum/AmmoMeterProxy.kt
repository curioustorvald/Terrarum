package net.torvald.terrarum

import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UIVitalMetre

/**
 * Created by SKYHi14 on 2017-04-21.
 */
object AmmoMeterProxy {

    operator fun invoke(actor: ActorHumanoid, meter: UIVitalMetre) {
        val currentItem = actor.inventory.itemEquipped[InventoryItem.EquipPosition.HAND_GRIP]

        if (currentItem == null) {
            meter.vitalGetterMax = { null }
            meter.vitalGetterVal = { null }
        }
        else {
            meter.vitalGetterVal = {
                if (ItemCodex[currentItem.originalID].consumable)
                actor.inventory.getByDynamicID(currentItem.dynamicID)!!.amount.toFloat()
            else
                actor.inventory.getByDynamicID(currentItem.dynamicID)!!.item.durability
            }

            meter.vitalGetterMax = {
                if (ItemCodex[currentItem.originalID].consumable)
                    500f
                else
                    actor.inventory.getByDynamicID(currentItem.dynamicID)!!.item.maxDurability.toFloat()
            }
        }
    }
}