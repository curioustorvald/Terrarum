package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-09-02.
 */
class Biomegen(world: GameWorld, isFinal: Boolean, seed: Long, params: Any, val biomeMapOut: HashMap<BlockAddress, Byte>) : Gen(world, isFinal, seed, params) {

    private val YHEIGHT_MAGIC = 2800.0 / 3.0
    private val YHEIGHT_DIVISOR = 2.0 / 7.0


    private val THISWORLD_SAND: ItemID
    private val THISWORLD_SANDSTONE: ItemID

    init {
        val SAND_BASE = getSandVariation(seed)
        THISWORLD_SAND = "basegame:" + (Block.SAND.substringAfter(':').toInt() + SAND_BASE)
        THISWORLD_SANDSTONE = "basegame:" + (Block.SANDSTONE.substringAfter(':').toInt() + SAND_BASE)
    }



    override fun getDone(loadscreen: LoadScreenBase?) {
        loadscreen?.let {
            it.stageValue += 1
            it.progress.set(0L)
        }

//        loadscreen.progress.set((loadscreen.progress.get() + 0x1_000000_000000L) and 0x7FFF_000000_000000L)

        Worldgen.threadExecutor.renew()
        submitJob(loadscreen)
        Worldgen.threadExecutor.join()
    }

    companion object {
        fun getSandVariation(seed: Long): Int {
            val SAND_RND = (seed shake "SANDYCOLOURS").ushr(7).xor(seed and 255L).and(255L).toInt()
            val SAND_BASE = when (SAND_RND) {
                255 -> 5 // green
                in 252..254 -> 4 // black
                in 245..251 -> 2 // red
                in 230..244 -> 1 // white
                else -> 0
            }

            return SAND_BASE
        }

        private const val slices = 5
        private val nearbyArr8 = arrayOf(
            (-1 to -1), // tileTL
            (+1 to -1), // tileTR
            (-1 to +1), // tileBL
            (+1 to +1), // tileBR
            (0 to -1), // tileT
            (0 to +1), // tileB
            (-1 to 0), // tileL
            (+1 to 0) // tileR
        )
        private val nearbyArr4 = arrayOf(
            (0 to -1), // tileT
            (0 to +1), // tileB
            (-1 to 0), // tileL
            (+1 to 0) // tileR
        )
        private const val TL8 = 0
        private const val TR8 = 1
        private const val BL8 = 2
        private const val BR8 = 3
        private const val TP8 = 4
        private const val BT8 = 5
        private const val LF8 = 6
        private const val RH8 = 7

        private const val TP4 = 0
        private const val BT4 = 1
        private const val LF4 = 2
        private const val RH4 = 3

        /* Biome key format:

        0x r 0000 ww

        r: rocky?
        ww: woods density

         */

        const val BIOME_KEY_PLAINS = 1.toByte()
        const val BIOME_KEY_SPARSE_WOODS = 2.toByte()
        const val BIOME_KEY_WOODLANDS = 3.toByte()
        const val BIOME_KEY_ROCKY = (-1).toByte()
        const val BIOME_KEY_SANDY = (-2).toByte()
        const val BIOME_KEY_GRAVELS = (-3).toByte()

    }

    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        for (x in xStart until xStart + CHUNK_W) {
            val sampleTheta = (x.toDouble() / world.width) * TWO_PI
            val sx = sin(sampleTheta) * soff + soff // plus sampleOffset to make only
            val sz = cos(sampleTheta) * soff + soff // positive points are to be sampled
            for (y in yStart until yStart + CHUNK_H) {
                val sy = Worldgen.getSY(y)

                val control1 =
                    noises[0].get(sx, sy, sz).coerceIn(0.0, 0.99999).times(slices).toInt().coerceAtMost(slices - 1)
                val control2 = noises[1].get(sx, sy, sz).coerceIn(0.0, 0.99999).times(9).toInt().coerceAtMost(9 - 1)
                val control3 = noises[2].get(sx, sy, sz).coerceIn(0.0, 0.99999).times(9).toInt().coerceAtMost(9 - 1)
                val ba = LandUtil.getBlockAddr(world, x, y)

                if (y > 0) {
                    val tileThis = world.getTileFromTerrain(x, y)
                    val wallThis = world.getTileFromWall(x, y)
                    val nearbyTerr = nearbyArr8.map { world.getTileFromTerrain(x + it.first, y + it.second) }
                    val nearbyWall = nearbyArr8.map { world.getTileFromWall(x + it.first, y + it.second) }
                    val exposedToAir = nearbyTerr.any { it == Block.AIR } && nearbyWall.any { it == Block.AIR }
                    val hasNoFloor = (nearbyTerr[BT8] == Block.AIR)

                    val grassRock = when (control1) {
                        0 -> { // woodlands
                            if (tileThis == Block.DIRT && exposedToAir) {
                                biomeMapOut[ba] = BIOME_KEY_WOODLANDS
                                Block.GRASS to null
                            }
                            else null to null
                        }

                        1 -> { // sparse forest
                            if (tileThis == Block.DIRT && exposedToAir) {
                                biomeMapOut[ba] = BIOME_KEY_SPARSE_WOODS
                                Block.GRASS to null
                            }
                            else null to null
                        }

                        2, 3 -> { // plains
                            if (tileThis == Block.DIRT && exposedToAir) {
                                biomeMapOut[ba] = BIOME_KEY_PLAINS
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
                                if (exposedToAir) biomeMapOut[ba] = BIOME_KEY_ROCKY
                                Block.STONE to Block.STONE
                            }
                            else null to null
                        }

                        else -> null to null
                    }
                    val sablum = when (control2) {
                        0 -> {
                            if (tileThis == Block.DIRT && hasNoFloor) {
                                if (exposedToAir) biomeMapOut[ba] = BIOME_KEY_GRAVELS
                                Block.STONE to null
                            }
                            else if (tileThis == Block.DIRT) {
                                if (exposedToAir) biomeMapOut[ba] = BIOME_KEY_GRAVELS
                                Block.GRAVEL to null
                            }
                            else null to null
                        }

                        8 -> {
                            if (tileThis == Block.DIRT && hasNoFloor) {
                                if (exposedToAir) biomeMapOut[ba] = BIOME_KEY_SANDY
                                THISWORLD_SANDSTONE to null
                            }
                            else if (tileThis == Block.DIRT) {
                                if (exposedToAir) biomeMapOut[ba] = BIOME_KEY_SANDY
                                THISWORLD_SAND to null
                            }
                            else null to null
                        }

                        else -> null to null
                    }
                    val lutum = when (control3) {
                        0 -> {
                            if (tileThis == Block.DIRT || wallThis == Block.DIRT) {
                                Block.CLAY to Block.CLAY
                            }
                            else null to null
                        }

                        else -> null to null
                    }

                    val outTile = if (grassRock.first == Block.STONE)
                        grassRock
                    else if (sablum.first != null)
                        sablum
                    else if (lutum.first != null)
                        lutum
                    else grassRock


                    if (outTile.first != null && tileThis != Block.AIR)
                        world.setTileTerrain(x, y, outTile.first!!, true)
                    if (outTile.second != null)
                        world.setTileWall(x, y, outTile.second!!, true)
                }
            }
        }
    }

    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        val params = params as BiomegenParams

        return listOf(
            makeRandomSpotties("TERRA", params.featureSize1),
            makeRandomSpotties("SABLUM", params.featureSize2),
            makeRandomSpotties("LUTUM", params.featureSize3),
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
    val featureSize2: Double = 120.0,
    val featureSize3: Double = 30.0,
)