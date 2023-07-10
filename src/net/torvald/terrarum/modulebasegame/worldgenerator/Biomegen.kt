package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.App
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-09-02.
 */
class Biomegen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {

    private val threadExecutor = TerrarumIngame.worldgenThreadExecutor

    private val genSlices = max(threadExecutor.threadCount, world.width / 8)

    private val YHEIGHT_MAGIC = 2800.0 / 3.0
    private val YHEIGHT_DIVISOR = 2.0 / 7.0

    override fun getDone() {
        threadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).map { xs ->
            threadExecutor.submit {
                val localJoise = getGenerator(seed, params as BiomegenParams)
                for (x in xs) {
                    for (y in 0 until world.height) {
                        val sampleTheta = (x.toDouble() / world.width) * TWO_PI
                        val sampleOffset = world.width / 8.0
                        val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                        val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                        val sampleY = y - (world.height - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant
                        // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
                        val noise = localJoise.map { it.get(sampleX, sampleY, sampleZ) }

                        draw(x, y, noise, world)
                    }
                }
            }
        }

        threadExecutor.join()

        App.printdbg(this, "Waking up Worldgen")
    }

    companion object {
        private const val slices = 5
        private val nearbyArr = arrayOf(
            (-1 to -1), // tileTL
            (+1 to -1), // tileTR
            (-1 to +1), // tileBL
            (+1 to +1), // tileBR
            (0 to -1), // tileT
            (0 to +1), // tileB
            (-1 to 0), // tileL
            (+1 to 0) // tileR
        )
        private const val TL = 0
        private const val TR = 1
        private const val BL = 2
        private const val BR = 3
        private const val TP = 4
        private const val BT = 5
        private const val LF = 6
        private const val RH = 7
    }

    private fun draw(x: Int, y: Int, noiseValue: List<Double>, world: GameWorld) {
        val control = noiseValue[0].coerceIn(0.0, 0.99999).times(slices).toInt().coerceIn(0 until slices)

        if (y > 0) {
            val tileThis = world.getTileFromTerrain(x, y)
            val wallThis = world.getTileFromWall(x, y)
            val nearbyTerr = nearbyArr.map { world.getTileFromTerrain(x + it.first, y + it.second) }
            val nearbyWall = nearbyArr.map { world.getTileFromWall(x + it.first, y + it.second) }

            when (control) {
                0 -> { // woodlands
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        world.setTileTerrain(x, y, Block.GRASS, true)
                    }
                }
                1 -> { // shrublands
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        world.setTileTerrain(x, y, Block.GRASS, true)
                    }
                }
                2, 3 -> { // plains
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        world.setTileTerrain(x, y, Block.GRASS, true)
                    }
                }
                /*3 -> { // sands
                    if (tileThis == Block.DIRT && (nearbyTerr[BT] == Block.STONE || nearbyTerr[BT] == Block.AIR)) {
                        world.setTileTerrain(x, y, Block.SANDSTONE)
                    }
                    else if (tileThis == Block.DIRT) {
                        world.setTileTerrain(x, y, Block.SAND)
                    }
                }*/
                4 -> { // rockylands
                    if (tileThis == Block.DIRT || tileThis == Block.STONE_QUARRIED) {
                        world.setTileTerrain(x, y, Block.STONE, true)
                        world.setTileWall(x, y, Block.STONE, true)
                    }
                }
            }
        }
    }

    private fun getGenerator(seed: Long, params: BiomegenParams): List<Joise> {
        //val biome = ModuleBasisFunction()
        //biome.setType(ModuleBasisFunction.BasisType.SIMPLEX)

        // simplex AND fractal for more noisy edges, mmmm..!
        val fractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.MULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setNumOctaves(4)
            it.setFrequency(1.0)
            it.seed = seed shake 0x7E22A
        }

        val scaleDomain = ModuleScaleDomain().also {
            it.setSource(fractal)
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
        }

        val scale = ModuleScaleOffset().also {
            it.setSource(scaleDomain)
            it.setOffset(1.0)
            it.setScale(1.0)
        }

        val last = scale

        return listOf(Joise(last))
    }

}

data class BiomegenParams(
        val featureSize: Double = 80.0
)