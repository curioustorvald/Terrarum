package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.App
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-09-02.
 */
class Biomegen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {

    private val YHEIGHT_MAGIC = 2800.0 / 3.0
    private val YHEIGHT_DIVISOR = 2.0 / 7.0


    private lateinit var THISWORLD_SAND: ItemID
    private lateinit var THISWORLD_SANDSTONE: ItemID


    override fun getDone(loadscreen: LoadScreenBase) {
        val SAND_RND = seed.ushr(7).xor(seed and 255L).and(255L).toInt()
        val SAND_BASE = when (SAND_RND) {
            255 -> 5 // green
            in 252..254 -> 4 // black
            in 245..251 -> 2 // red
            in 230..244 -> 1 // white
            else -> 0
        }
        THISWORLD_SAND = "basegame:" + (Block.SAND.substringAfter(':').toInt() + SAND_BASE)
        THISWORLD_SANDSTONE = "basegame:" + (Block.SANDSTONE.substringAfter(':').toInt() + SAND_BASE)


//        loadscreen.progress.set((loadscreen.progress.get() + 0x1_000000_000000L) and 0x7FFF_000000_000000L)

        Worldgen.threadExecutor.renew()
        (0 until world.width).sliceEvenly(Worldgen.genSlices).map { xs ->
            Worldgen.threadExecutor.submit {
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

        Worldgen.threadExecutor.join()

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
        val control1 = noiseValue[0].coerceIn(0.0, 0.99999).times(slices).toInt().coerceAtMost(slices - 1)
        val control2 = noiseValue[1].coerceIn(0.0, 0.99999).times(9).toInt().coerceAtMost(9 - 1)

        if (y > 0) {
            val tileThis = world.getTileFromTerrain(x, y)
            val wallThis = world.getTileFromWall(x, y)
            val nearbyTerr = nearbyArr.map { world.getTileFromTerrain(x + it.first, y + it.second) }
            val nearbyWall = nearbyArr.map { world.getTileFromWall(x + it.first, y + it.second) }

            val grassRock = when (control1) {
                0 -> { // woodlands
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        Block.GRASS to null
                    }
                    else null to null
                }
                1 -> { // shrublands
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        Block.GRASS to null
                    }
                    else null to null
                }
                2, 3 -> { // plains
                    if (tileThis == Block.DIRT && nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }) {
                        Block.GRASS to null
                    }
                    else null to null
                }
                /*3 -> { // sands
                    if (tileThis == Block.DIRT && (nearbyTerr[BT] == Block.STONE || nearbyTerr[BT] == Block.AIR)) {
                        world.setTileTerrain(x, y, Block.SANDSTONE, true)
                    }
                    else if (tileThis == Block.DIRT) {
                        world.setTileTerrain(x, y, Block.SAND, true)
                    }
                }*/
                4 -> { // rockylands
                    if (tileThis == Block.DIRT || tileThis == Block.STONE_QUARRIED) {
                        Block.STONE to Block.STONE
                    }
                    else null to null
                }
                else -> null to null
            }
            val sablum = when (control2) {
                0 -> {
                    if (tileThis == Block.DIRT && (nearbyTerr[BT] == Block.AIR)) {
                        Block.STONE_QUARRIED to null
                    }
                    else if (tileThis == Block.DIRT) {
                        Block.GRAVEL to null
                    }
                    else null to null
                }
                8 -> {
                    if (tileThis == Block.DIRT && (nearbyTerr[BT] == Block.AIR)) {
                        THISWORLD_SANDSTONE to null
                    }
                    else if (tileThis == Block.DIRT) {
                        THISWORLD_SAND to null
                    }
                    else null to null
                }
                else -> null to null
            }

            val outTile = if (grassRock.first == Block.STONE)
                grassRock
            else if (sablum.first != null)
                sablum
            else grassRock


            if (outTile.first != null)
                world.setTileTerrain(x, y, outTile.first!!, true)
            if (outTile.second != null)
                world.setTileWall(x, y, outTile.second!!, true)
        }
    }

    private fun getGenerator(seed: Long, params: BiomegenParams): List<Joise> {
        return listOf(
            makeRandomSpotties("TERRA", params.featureSize1),
            makeRandomSpotties("SABLUM", params.featureSize2),
        )
    }

    private fun makeRandomSpotties(shakeValue: String, featureSize: Double): Joise {
        //val biome = ModuleBasisFunction()
        //biome.setType(ModuleBasisFunction.BasisType.SIMPLEX)

        // simplex AND fractal for more noisy edges, mmmm..!
        val fractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.MULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setNumOctaves(4)
            it.setFrequency(1.0)
            it.seed = seed shake shakeValue
        }

        val scaleDomain = ModuleScaleDomain().also {
            it.setSource(fractal)
            it.setScaleX(1.0 / featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / featureSize)
            it.setScaleZ(1.0 / featureSize)
        }

        val scale = ModuleScaleOffset().also {
            it.setSource(scaleDomain)
            it.setOffset(1.0)
            it.setScale(1.0)
        }

        val last = scale
        return Joise(last)
    }

}

data class BiomegenParams(
    val featureSize1: Double = 80.0,
    val featureSize2: Double = 120.0
)