package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_PLAINS
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_SHRUBLANDS
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_WOODLANDS
import net.torvald.terrarum.realestate.LandUtil
import kotlin.math.absoluteValue

/**
 * Created by minjaesong on 2023-11-10.
 */
class Treegen(world: GameWorld, seed: Long, params: TreegenParams, val biomeMap: HashMap<BlockAddress, Byte>) : Gen(world, seed, params) {

    override fun getDone(loadscreen: LoadScreenBase) {
        loadscreen.stageValue += 1
        loadscreen.progress.set(0L)

        Worldgen.threadExecutor.renew()
        (0 until world.width).sliceEvenly(Worldgen.genSlices).rearrange().mapIndexed { i, xs ->
            Worldgen.threadExecutor.submit {
                tryToPlant(xs, makeGrassMap(xs))
                loadscreen.progress.addAndGet((xs.last - xs.first + 1).toLong())
            }
        }

        Worldgen.threadExecutor.join()

        App.printdbg(this, "Waking up Worldgen")
    }


    private val treegenProbabilityToBiome = hashMapOf(
        0.toByte() to 0.0,
        BIOME_KEY_WOODLANDS to 1.0/params.woodlandsTreeDist,
        BIOME_KEY_SHRUBLANDS to 1.0/params.shrublandsTreeDist,
        BIOME_KEY_PLAINS to 1.0/params.plainsTreeDist,
    )


    private fun makeGrassMap(xs: IntProgression): Array<List<Int>> {
        val r = Array<List<Int>>(xs.last - xs.first + 1) { emptyList() }


        for (x in xs) {
            val ys = ArrayList<Int>()
            var y = 1
            var tileUp = world.getTileFromTerrain(x, y - 1)
            var tile = world.getTileFromTerrain(x, y)
            var tileProp = BlockCodex[tile]
            while (y < 800) {
                if (tileProp.hasAnyTagOf("ROCK", "STONE")) break

                if (tile == Block.GRASS && tileUp == Block.AIR) {
                    ys.add(y)
                }

                y += 1

                tileUp = tile
                tile = world.getTileFromTerrain(x, y)
                tileProp = BlockCodex[tile]
            }

            r[x - xs.first] = ys
        }

        return r
    }

    private fun tryToPlant(xs: IntProgression, grassMap: Array<List<Int>>) {
        for (x in xs.first+1..xs.last-1) {
            grassMap[x - xs.first].forEachIndexed { index, y ->
                val yLeft = grassMap[x - xs.first - 1].getOrNull(index) ?: -1
                val yRight = grassMap[x - xs.first + 1].getOrNull(index) ?: -1

                val grad1 = y - yLeft
                val grad2 = yRight - y

                if ((grad1 * grad2).absoluteValue <= 1) {
//                    printdbg(this, "Trying to plant tree at $x, $y")

                    val rnd = Math.random()
                    val biome = biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0
                    val prob = treegenProbabilityToBiome[biome] ?: 0.0

                    // actually plant a tree
                    if (prob > 0.0 && rnd < prob) {
                        plantTree(x, y, 0, 1) // TODO randomly select species and small/large tree
                    }
                }
            }
        }
    }

    // don't use POI -- generate tiles using code for randomisation
    /**
     * @param y where the grass/dirt tile is
     */
    private fun plantTree(x: Int, y: Int, type: Int, size: Int) {
        val trunk = "basegame:" + ((if (size >= 1) 64 else 72) + type)
        val foliage = "basegame:" + (112 + type)

        var growCnt = 1
        if (size == 1) {
            var heightSum = 5+3+2
            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1) {
                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            // roll for dice until we get a height that fits into the given terrain
            var stem=0; var bulb1=0; var bulb2=0; var bulb3=0;
//            do {
                stem = 7 + fudgeN(2)
                bulb1 = 4 + fudgeN(1)
                bulb2 = 3 + fudgeN(1)
                bulb3 = 2 + fudgeN(1)
                heightSum = stem + bulb1 + bulb2 + bulb3
//            }
//            while ((1..heightSum).none { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid })

            printdbg(this, "Planting tree at $x, $y; params: $stem, $bulb1, $bulb2, $bulb3")

            // trunk
            for (i in 0 until stem) {
                for (xi in -1..+1) {
                    if (xi != 0) {
                        val tileHere = world.getTileFromTerrain(x + xi, y - growCnt)
                        if (BlockCodex[tileHere].hasTag("TREETRUNK"))
                            world.setTileTerrain(x + xi, y - growCnt, Block.AIR, true)
                    }
                    else {
                        world.setTileTerrain(x + xi, y - growCnt, trunk, true)
                    }
                }
                growCnt += 1
            }
            // bulb base
            for (x in x-2..x+2) {
                val tileHere = world.getTileFromTerrain(x, y - growCnt)
                if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                    world.setTileTerrain(x, y - growCnt, foliage, true)
            }
            growCnt += 1
            // bulb 1
            for (i in 0 until bulb1) {
                for (x in x-3..x+3) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
            // bulb 2
            for (i in 0 until bulb2) {
                for (x in x-2..x+2) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
            // bulb 3
            for (i in 0 until bulb3) {
                for (x in x-1..x+1) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }

        }
        else throw IllegalArgumentException("Unknown tree size: $size")
    }

    /**
     * Rearranges the list such that:
     * `1,2,3,4,5,6,7,8,9`
     * is ordered as:
     * `1,5,9,2,6,3,7,4,8`
     */
    private fun List<IntProgression>.rearrange(): List<IntProgression> {
        val r = ArrayList<IntProgression>()
        val stride = this.size / 2

        for (i in 0 until stride) {
            var b = i
            while (b < this.size) {
                r.add(this[b])
                b += stride
            }
        }

        return r
    }

    /**
     * @return normally distributed integer, for `maxvar=1`, `[-1, 0, 1]`; for `maxvar=2`, `[-2, -1, 0, 1, 2]`, etc.
     */
    private fun fudgeN(maxvar: Int) = (0 until maxvar).sumOf { (Math.random() * 3).toInt() - 1 }

}

data class TreegenParams(
    val woodlandsTreeDist: Int = 9, // distances are merely a suggestion tho
    val shrublandsTreeDist: Int = 12,
    val plainsTreeDist: Int = 16,
)