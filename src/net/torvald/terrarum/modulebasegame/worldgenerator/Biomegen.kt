package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2019-09-02.
 */
class Biomegen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {
    override var generationStarted: Boolean
        get() = TODO("not implemented")
        set(value) {}
    override val generationDone: Boolean
        get() = TODO("not implemented")

    override fun run() {
        TODO("not implemented")
    }
}

data class BiomegenParams(
        val featureSize: Double = 80.0
)