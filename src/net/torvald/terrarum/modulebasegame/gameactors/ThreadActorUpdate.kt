package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2016-05-25.
 */
class ThreadActorUpdate(val startIndex: Int, val endIndex: Int) : Runnable {
    override fun run() {
        for (i in startIndex..endIndex) {
            val it = Terrarum.ingame!!.actorContainerActive[i]
            it.update(AppLoader.UPDATE_RATE.toFloat())

            if (it is Pocketed) {
                it.inventory.forEach { inventoryEntry ->
                    inventoryEntry.item.effectWhileInPocket(AppLoader.UPDATE_RATE.toFloat())
                    if (it.equipped(inventoryEntry.item)) {
                        inventoryEntry.item.effectWhenEquipped(AppLoader.UPDATE_RATE.toFloat())
                    }
                }
            }
        }
    }
}