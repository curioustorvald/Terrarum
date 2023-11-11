package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.App
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
        val STRIDE = 4
        val r = Array<List<Int>>(xs.last - xs.first + 1) { emptyList() }


        for (x in xs) {
            val ys = ArrayList<Int>()
            var y = 0
            while (y < 800) {
                val tile = world.getTileFromTerrain(x, y)
                val tileProp = BlockCodex[tile]

                if (tileProp.hasAnyTagOf("ROCK", "STONE")) break

                if (tile == Block.GRASS) {
                    ys.add(y)
                }
                // if dirt was hit, climb back up until a grass is seen
                else if (tile == Block.DIRT) {
                    var yi = 1
                    var tile0 = world.getTileFromTerrain(x, y - yi)
                    var found = false
                    while (tile0 == Block.DIRT || yi < STRIDE) {
                        tile0 = world.getTileFromTerrain(x, y - yi)
                        if (tile0 == Block.GRASS) found = true
                        yi += 1
                    }

                    // filter duplicates
                    if (found && ys.last() != y - yi) {
                        ys.add(y - yi)
                    }
                }

                y += STRIDE
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
                    val rnd = Math.random()
                    val biome = biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0
                    val prob = treegenProbabilityToBiome[biome]!!

                    // actually plant a tree
                    if (rnd < prob) {

                    }
                }
            }


            for (y in grassMap[x - xs.first]) {
            }
        }
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
}

data class TreegenParams(
    val woodlandsTreeDist: Int = 9, // distances are merely a suggestion tho
    val shrublandsTreeDist: Int = 14,
    val plainsTreeDist: Int = 21,
)