package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.LoadScreen
import net.torvald.terrarum.gameworld.GameWorld

/**
 * New world generator.
 *
 * Created by minjaesong on 2019-09-02.
 */
object Worldgen {

    operator fun invoke(worldIndex: Int, params: WorldgenParams) {
        val genTime = AppLoader.getTIME_T()
        val world = GameWorld(worldIndex, params.width, params.height, genTime, genTime, 0)

        val jobs = listOf(
                Work("Reticulating Splines", Terragen(world, params.seed, params.terragenParams))
                //Work("Adding Vegetations") { Biomegen(world, params.seed, params.biomegenParams) }
        )


        for (i in 0 until jobs.size) {
            val it = jobs[i]

            LoadScreen.addMessage(it.loadingScreenName)
            it.theWork.run()

            // busy wait
            while (!it.theWork.generationDone) { }
        }

    }

    private data class Work(val loadingScreenName: String, val theWork: Gen)

}

abstract class Gen(val world: GameWorld, val seed: Long, val params: Any) {
    abstract var generationStarted: Boolean
    abstract val generationDone: Boolean
    open fun run() {
        if (generationDone) {
            // worldgen.wake()
        }
    }
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