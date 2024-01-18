package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import net.torvald.random.XXHash64
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isOre
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.OrePlacement
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import kotlin.math.max

/**
 * Created by minjaesong on 2023-10-26.
 */
class OregenAutotiling(world: GameWorld, isFinal: Boolean, seed: Long, val tilingModes: HashMap<ItemID, String>) : Gen(world, isFinal, seed) {

    override fun getDone(loadscreen: LoadScreenBase?) {
        Worldgen.threadExecutor.renew()
        submitJob(null)
        Worldgen.threadExecutor.join()
    }

    private fun getHashCoord(x: Int, y: Int, mod: Int): Int {
        val (x, y) = world.coerceXY(x, y)
        return (XXHash64.hash(LandUtil.getBlockAddr(world, x, y).toBig64(), ((x*16777619) xor (y+1073)).toLong()) fmod mod.toLong()).toInt()
    }

    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        for (x in xStart until xStart + CHUNK_W) {
            for (y in yStart until yStart + CHUNK_H) {

                val (ore, _) = world.getTileFromOre(x, y)

                if (ore.isOre()) {
                    // get tiling mode
                    val tilingMode = tilingModes[ore]

                    val placement = when (tilingMode) {
                        "a16" -> {
                            // get placement (tile connection) info
                            val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                                acc or (placement.item == ore).toInt(index)
                            }
                            BlocksDrawer.connectLut16[autotiled]

                        }

                        "a16x4" -> {
                            // get placement (tile connection) info
                            val mult = getHashCoord(x, y, 4)
                            val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                                acc or (placement.item == ore).toInt(index)
                            }
                            BlocksDrawer.connectLut16[autotiled] or mult.shl(4)
                        }

                        "a16x16" -> {
                            // get placement (tile connection) info
                            val mult = getHashCoord(x, y, 16)
                            val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                                acc or (placement.item == ore).toInt(index)
                            }
                            BlocksDrawer.connectLut16[autotiled] or mult.shl(4)
                        }

                        "a47" -> {
                            // get placement (tile connection) info
                            val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                                acc or (placement.item == ore).toInt(index)
                            }
                            BlocksDrawer.connectLut47[autotiled]
                        }

                        "r16" -> {
                            getHashCoord(x, y, 16)
                        }

                        "r8" -> {
                            getHashCoord(x, y, 8)
                        }

                        else -> throw IllegalArgumentException("Unknown tiling mode: $tilingMode")
                    }


                    // actually put the ore block
                    world.setTileOre(x, y, ore, placement) // autotiling will be handled by the other worldgen process
                }
            }
        }
    }

    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        return emptyList()
    }

    private fun getNearbyTilesPos8(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x + 1, y + 1),
            Point2i(x, y + 1),
            Point2i(x - 1, y + 1),
            Point2i(x - 1, y),
            Point2i(x - 1, y - 1),
            Point2i(x, y - 1),
            Point2i(x + 1, y - 1)
        )
    }
    private fun getNearbyOres8(x: Int, y: Int): List<OrePlacement> {
        return getNearbyTilesPos8(x, y).map { world.getTileFromOre(it.x, it.y) }
    }

}