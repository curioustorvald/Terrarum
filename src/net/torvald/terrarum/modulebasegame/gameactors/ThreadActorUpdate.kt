package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import java.util.concurrent.Callable
import net.torvald.terrarum.*

/**
 * Created by minjaesong on 2016-05-25.
 */
class ThreadActorUpdate(val startIndex: Int, val endIndex: Int) : Callable<Unit> {
    override fun call() {
        for (i in startIndex..endIndex) {
            val it = INGAME.actorContainerActive[i]
            it.update(App.UPDATE_RATE)

            if (it is Pocketed) {
                it.inventory.forEach { inventoryEntry ->
                    ItemCodex[inventoryEntry.itm]?.effectWhileInPocket(App.UPDATE_RATE)
                    if (it.equipped(inventoryEntry.itm)) {
                        ItemCodex[inventoryEntry.itm]?.effectWhenEquipped(App.UPDATE_RATE)
                    }
                }
            }
        }
    }
}