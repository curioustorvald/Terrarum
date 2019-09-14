package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.gameworld.GameWorld

/**
 * New world generator.
 *
 * Created by minjaesong on 2019-09-02.
 */
object Worldgen {

    operator fun invoke(worldIndex: Int, params: WorldgenParams) {

        val world = GameWorld(worldIndex, params.width, params.height, System.currentTimeMillis() / 1000, System.currentTimeMillis() / 1000, 0)

        val jobs = listOf(
                Work("Reticulating Splines") { Terragen(world, params.seed, params.terragenParams) },
                Work("Adding Vegetations") { Biomegen(world, params.seed, params.biomegenParams) }
        )

    }

    private data class Work(val loadingScreenName: String, val theWork: () -> Unit)

}

interface Gen {
    var generationStarted: Boolean
    val generationDone: Boolean
    operator fun invoke(world: GameWorld, seed: Long, params: Any)
}

data class WorldgenParams(
        val width: Int,
        val height: Int,
        val seed: Long,
        // optional parametres
        val terragenParams: TerragenParams = TerragenParams(),
        val biomegenParams: BiomegenParams = BiomegenParams()
)

infix fun Long.shake(other: Long): Long {
    var s0 = this
    var s1 = other

    s1 = s1 xor s0
    s0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14)
    s1 = s1 shl 36 or s1.ushr(28)

    return s0 + s1
}

val TWO_PI = Math.PI * 2.0