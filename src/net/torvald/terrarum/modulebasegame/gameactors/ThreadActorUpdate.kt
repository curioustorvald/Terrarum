package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 2016-05-25.
 */
class ThreadActorUpdate(val startIndex: Int, val endIndex: Int) : Runnable {
    override fun run() {
        for (i in startIndex..endIndex) {
            val it = Terrarum.ingame!!.actorContainerActive[i]
            it.update(AppLoader.UPDATE_RATE)

            if (it is Pocketed) {
                it.inventory.forEach { inventoryEntry ->
                    ItemCodex[inventoryEntry.item]?.effectWhileInPocket(AppLoader.UPDATE_RATE)
                    if (it.equipped(inventoryEntry.item)) {
                        ItemCodex[inventoryEntry.item]?.effectWhenEquipped(AppLoader.UPDATE_RATE)
                    }
                }
            }
        }
    }
}