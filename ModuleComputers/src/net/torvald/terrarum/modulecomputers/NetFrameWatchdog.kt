package net.torvald.terrarum.modulecomputers

import net.torvald.terrarum.App
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumWorldWatchdog
import net.torvald.terrarum.modulebasegame.gameworld.NetRunner

/**
 * Created by minjaesong on 2025-03-02.
 */
class NetFrameWatchdog : TerrarumWorldWatchdog(App.TICK_SPEED * 60) {
    override fun invoke(world: GameWorld) {
        (world.extraFields["tokenring"] as NetRunner).let {
            it.purgeDeadFrames()
        }
    }
}