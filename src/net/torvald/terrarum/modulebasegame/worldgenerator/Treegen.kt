package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_PLAINS
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_SPARSE_WOODS
import net.torvald.terrarum.modulebasegame.worldgenerator.Biomegen.Companion.BIOME_KEY_WOODLANDS
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_DIVISOR
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.toUint
import kotlin.math.absoluteValue

/**
 * Created by minjaesong on 2023-11-10.
 */
class Treegen(world: GameWorld, seed: Long, val terragenParams: TerragenParams, params: TreegenParams, val biomeMap: HashMap<BlockAddress, Byte>) : Gen(world, seed, params) {

    override fun getDone(loadscreen: LoadScreenBase) {
        loadscreen.stageValue += 1
        loadscreen.progress.set(0L)

        Worldgen.threadExecutor.renew()
        (0 until world.width).sliceEvenly(Worldgen.genSlices).rearrange().mapIndexed { i, xs ->
            Worldgen.threadExecutor.submit {
                tryToPlant(xs, makeGrassMap(xs), HQRNG(seed shake xs.last.toLong()))
                loadscreen.progress.addAndGet((xs.last - xs.first + 1).toLong())
            }
        }

        Worldgen.threadExecutor.join()

        App.printdbg(this, "Waking up Worldgen")
    }

    private fun makeGrassMap(xs: IntProgression): Array<List<Int>> {
        val r = Array<List<Int>>(xs.last - xs.first + 1) { emptyList() }
        val ymax = (world.height * YHEIGHT_DIVISOR + terragenParams.featureSize).ceilToInt()

        for (x in xs) {
            val ys = ArrayList<Int>()
            var y = (world.height * YHEIGHT_DIVISOR - terragenParams.featureSize).floorToInt().coerceAtLeast(1)
            var tileUp = world.getTileFromTerrain(x, y - 1)
            var tile = world.getTileFromTerrain(x, y)
            while (y < ymax) {
                if (tile == Block.GRASS && tileUp == Block.AIR) {
                    ys.add(y)
                }

                y += 1

                tileUp = tile
                tile = world.getTileFromTerrain(x, y)
            }

            r[x - xs.first] = ys
        }

        return r
    }

    private val treePlot1 = arrayOf(2, 3)
    private val treePlot2 = arrayOf(6, 7)
    private val treePlotM = arrayOf(4, 5)

    private fun Double.toDitherredInt(rng: HQRNG): Int {
        val ibase = this.floorToInt()
        val thre = this - ibase
        return if (rng.nextDouble() < (1.0 - thre)) ibase else ibase + 1
    }

    private fun tryToPlant(xs: IntProgression, grassMap: Array<List<Int>>, rng: HQRNG) {
        val treeSpecies = 0


        // single "slice" is guaranteed to be 9 blocks wide
        val treePlantable = grassMap.zip(xs.first..xs.last).flatMap { (ys, x) ->
            ys.map { y -> x to y }
        }
        // larger value = more likely to spawn large tree
        // range: [0, 3]
        val woodsWgtD = treePlantable.map { (x, y) -> (biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0).toUint().and(3) }.average()
        val woodsWgt = treePlantable.map { (x, y) -> (biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0).toUint().and(3) }.average().toDitherredInt(rng)

        val treeToSpawn = when (woodsWgt) { // . - none (0) o - shrub (1) ! - small tree (2) $ - large tree (3)
            // 0: . .
            // 1:  o
            // 2: ! . / . !
            // 3: ! ! /  $
            3 -> {
                val tree = if (rng.nextDouble() > 0.5)
                    listOf(3)
                else
                    listOf(2, 2)

                if (rng.nextDouble() < (params as TreegenParams).deepForestTreeProb) tree else listOf()
            }
            2 -> {
                val tree = if (rng.nextDouble() > 0.5)
                    listOf(0, 2)
                else
                    listOf(2, 0)

                if (rng.nextDouble() < (params as TreegenParams).sparseForestTreeProb) tree else listOf()
            }
            1 -> {
                val tree = listOf(1)

                if (rng.nextDouble() < (params as TreegenParams).plainsShrubProb) tree else listOf()
            }
            else -> listOf()
        }

//        printdbg(this, "Tree to spawn at [${xs.first}..${xs.last}]: $treeToSpawn (woodsWgt=$woodsWgt/$woodsWgtD)")

        when (treeToSpawn.size) {
            2 -> {
                val plot1 = if (treeToSpawn[0] < 3) treePlot1.random() else treePlot1[0]
                val plot2 = if (treeToSpawn[1] < 3) treePlot2.random() else treePlot2[0]

                // if there is no grass, grassMap[x] is an empty list
                if (treeToSpawn[0] != 0) {
                    grassMap[plot1].let { if (it.isEmpty()) null else it.random() }?.let {
                        plantTree(xs.first + plot1, it, treeSpecies, 1, rng) // TODO use treeSize from the treeToSpawn
                    }
                }
                if (treeToSpawn[1] != 0) {
                    grassMap[plot2].let { if (it.isEmpty()) null else it.random() }?.let {
                        plantTree(xs.first + plot2, it, treeSpecies, 1, rng) // TODO use treeSize from the treeToSpawn
                    }
                }
            }
            1 -> {
                val plot1 = if (treeToSpawn[0] < 3) treePlotM.random() else treePlotM[0]

                // if there is no grass, grassMap[x] is an empty list
                if (treeToSpawn[0] != 0) {
                    val treeSize = arrayOf(null, 0, 1, 2)[treeToSpawn[0]]
                    grassMap[plot1].let { if (it.isEmpty()) null else it.random() }?.let {
                        plantTree(xs.first + plot1, it, treeSpecies, treeSize!!, rng)
                    }
                }
            }
        }


        /*for (x in xs.first+1..xs.last-1) {
            grassMap[x - xs.first].forEachIndexed { index, y ->
                val yLeft = grassMap[x - xs.first - 1].getOrNull(index) ?: -1
                val yRight = grassMap[x - xs.first + 1].getOrNull(index) ?: -1

                val grad1 = y - yLeft
                val grad2 = yRight - y

                if ((grad1 * grad2).absoluteValue <= 1) {
//                    printdbg(this, "Trying to plant tree at $x, $y")

                    val rnd = rng.nextDouble()
                    val biome = biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0
                    val prob = treegenProbabilityToBiome[biome] ?: 0.0

                    // actually plant a tree
                    if (prob > 0.0 && rnd < prob) {
                        plantTree(x, y, 0, 1) // TODO randomly select species and small/large tree
                    }
                }
            }
        }*/
    }

    // don't use POI -- generate tiles using code for randomisation
    /**
     * @param y where the grass/dirt tile is
     */
    private fun plantTree(x: Int, y: Int, type: Int, size: Int, rng: HQRNG) {
        val trunk = "basegame:" + ((if (size <= 1) 64 else 72) + type)
        val foliage = "basegame:" + (112 + type)

        var growCnt = 1
        if (size == 0) {
            val heightSum = 3

            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1) {
                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            val stem = 1
            val bulb1 = 3 + fudgeN(1, rng)

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
            // bulb 1
            for (i in 0 until bulb1) {
                for (x in x-1..x+1) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
        }
        else if (size == 1) {
            val heightSum = 5+3+2+1
            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1) {
                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            // roll for dice until we get a height that fits into the given terrain
            val stem = 7 + fudgeN(2, rng)
            val bulb1 = 4 + fudgeN(1, rng)
            val bulb2 = 3 + fudgeN(1, rng)
            val bulb3 = 2 + fudgeN(1, rng)
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
        else if (size == 2) {
            val heightSum = 12+4+3+2+1
            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP2 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1 || chkP2) {
                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            // roll for dice until we get a height that fits into the given terrain
            val stem = 15 + fudgeN(3, rng)
            val bulb1 = 5 + fudgeN(1, rng)
            val bulb2 = 4 + fudgeN(1, rng)
            val bulb3 = 3 + fudgeN(1, rng)
            val bulb4 = 2 + fudgeN(1, rng)

            printdbg(this, "Planting tree at $x, $y; params: $stem, $bulb1, $bulb2, $bulb3")

            // soiling
            for (i in 1..2) {
                val tileLeft = world.getTileFromTerrain(x, y + i)
                val wallLeft = world.getTileFromWall(x, y + i)
                val tileRight = world.getTileFromTerrain(x + 1, y + i)
                if (tileRight != tileLeft) {
                    world.setTileTerrain(x + 1, y + i, tileLeft, true)
                    world.setTileWall(x + 1, y + i, wallLeft, true)
                }
            }
            // trunk
            for (i in 0 until stem) {
                for (xi in -1..+2) {
                    if (xi !in 0..1) {
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
            for (x in x-2..x+3) {
                val tileHere = world.getTileFromTerrain(x, y - growCnt)
                if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                    world.setTileTerrain(x, y - growCnt, foliage, true)
            }
            growCnt += 1
            for (x in x-3..x+4) {
                val tileHere = world.getTileFromTerrain(x, y - growCnt)
                if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                    world.setTileTerrain(x, y - growCnt, foliage, true)
            }
            growCnt += 1
            // bulb 1
            for (i in 0 until bulb1) {
                for (x in x-4..x+5) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
            // bulb 2
            for (i in 0 until bulb2) {
                for (x in x-3..x+4) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
            // bulb 3
            for (i in 0 until bulb3) {
                for (x in x-2..x+3) {
                    val tileHere = world.getTileFromTerrain(x, y - growCnt)
                    if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                        world.setTileTerrain(x, y - growCnt, foliage, true)
                }
                growCnt += 1
            }
            // bulb 4
            for (i in 0 until bulb4) {
                for (x in x-1..x+2) {
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
    private fun fudgeN(maxvar: Int, rng: HQRNG) = (0 until maxvar).sumOf { (rng.nextDouble() * 3).toInt() - 1 }

}

data class TreegenParams(
    val deepForestTreeProb: Double = 0.8,
    val sparseForestTreeProb: Double = 0.5,
    val plainsShrubProb: Double = 0.25,
)