package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_DIVISOR
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_MAGIC
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Created by minjaesong on 2023-10-25.
 */
class Oregen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {

    private val threadExecutor = TerrarumIngame.worldgenThreadExecutor
    private val genSlices = max(threadExecutor.threadCount, world.width / 8)

    override fun getDone() {
        threadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).mapIndexed { i, xs ->
            threadExecutor.submit {
                val localJoise = getGenerator(seed, params as OregenParams)
                for (x in xs) {
                    val sampleTheta = (x.toDouble() / world.width) * TWO_PI
                    val sampleOffset = world.width / 8.0
                    draw(x, localJoise, sampleTheta, sampleOffset)
                }
            }
        }

        threadExecutor.join()
    }

    /**
     * @return List of noise instances, each instance refers to one of the spawnable ores
     */
    private fun getGenerator(seed: Long, params: OregenParams): List<Joise> {
        TODO()
    }

    private fun draw(x: Int, noises: List<Joise>, st: Double, soff: Double) {
        for (y in 0 until world.height) {
            val sx = sin(st) * soff + soff // plus sampleOffset to make only
            val sz = cos(st) * soff + soff // positive points are to be sampled
            val sy = y - (world.height - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant
            // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant

            // get the actual noise values
            val noiseValues = noises.map { it.get(sx, sy, sz) }

            TODO()
        }
    }
}

data class OregenParams(
    val oreShapeFreq: Double = 0.04, //adjust the "density" of the caves
    val oreAttenuateBias: Double = 0.90, // adjust the "concentration" of the cave gen. Lower = larger voids
    val oreSelectThre: Double = 0.918, // also adjust this if you've touched the bias value. Number can be greater than 1.0
)