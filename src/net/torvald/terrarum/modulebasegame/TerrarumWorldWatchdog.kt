package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.gameworld.GameWorld

/**
 * @param runIntervalByTick how often should the watchdog run. 1: every single tick, 2: every other tick, 60: every second (if tickrate is 60)
 *
 * Created by minjaesong on 2025-03-02
 */
abstract class TerrarumWorldWatchdog(val runIntervalByTick: Int) {
    abstract operator fun invoke(world: GameWorld)
}
