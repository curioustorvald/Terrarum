package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.*

/**
 * Created by minjaesong on 2017-04-21.
 */
object AmmoMeterProxy {

    operator fun invoke(actor: ActorHumanoid, meter: UIVitalMetre) {
        val currentItem = ItemCodex[actor.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]]

        if (currentItem == null) {
            meter.vitalGetterMax = { null }
            meter.vitalGetterVal = { null }
        }
        else {
            meter.vitalGetterVal = {
                if (currentItem.stackable && currentItem.maxDurability == GameItem.DURABILITY_NA) {
                    actor.inventory.invSearchByDynamicID(currentItem.dynamicID)!!.qty.toFloat()
                }
                else
                    currentItem.durability
            }

            meter.vitalGetterMax = {
                if (currentItem.stackable && currentItem.maxDurability == GameItem.DURABILITY_NA)
                    500f
                else
                    currentItem.maxDurability.toFloat()
            }
        }
    }
}