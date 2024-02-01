package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import net.torvald.random.HQRNG
import net.torvald.random.XXHash32
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_DIVISOR
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.toUint

/**
 * Created by minjaesong on 2023-11-10.
 */
class Treegen(world: GameWorld, isFinal: Boolean, seed: Long, val terragenParams: TerragenParams, params: TreegenParams, val biomeMap: HashMap<BlockAddress, Byte>) : Gen(world, isFinal, seed, params) {

    override fun getDone(loadscreen: LoadScreenBase?) {
        loadscreen?.let {
            it.stageValue += 1
            it.progress.set(0L)
        }

        Worldgen.threadExecutor.renew()
        submitJob(loadscreen)
        Worldgen.threadExecutor.join()
    }


    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        val phi = (1.0 + Math.sqrt(5.0)) / 2.0
        for (i in 0 until 10) {
            val xs = (xStart + 9*i) until (xStart + 9*i) + 9
            tryToPlant(xs, seed.shake(java.lang.Double.doubleToLongBits(phi * (i + 1))).toInt(), makeGrassMap(xs))
        }
    }

    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        return emptyList()
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

    private val treePlot1 = intArrayOf(2, 3)
    private val treePlot2 = intArrayOf(6, 7)
    private val treePlotM = intArrayOf(4, 5)

    private fun IntArray.takeRand(x: Int, y: Int, h: Int): Int {
        val r = ((XXHash32.hashGeoCoord(x, y) * 31 + h) and 0xFFFFFF) / 16777216f
        return this[(r * this.size).toInt()]
    }

    private fun List<Int>.takeRand(x: Int, y: Int, h: Int): Int {
        val r = ((XXHash32.hashGeoCoord(x, y) * 31 + h) and 0xFFFFFF) / 16777216f
        return this[(r * this.size).toInt()]
    }

    private fun Double.toDitherredInt(x: Int, y: Int, h: Int): Int {
        val ibase = this.floorToInt()
        val thre = this - ibase
        return if (nextDouble(x, y, h) < 1.0 - thre) ibase else ibase + 1
    }

    private fun nextDouble(x: Int, y: Int, h: Int): Double {
        return ((XXHash32.hashGeoCoord(x, y) * 31 + h) and 0xFFFFFF) / 16777216.0
    }
    private fun nextFloat(x: Int, y: Int, h: Int): Float {
        return ((XXHash32.hashGeoCoord(x, y) * 31 + h) and 0xFFFFFF) / 16777216f
    }

    private fun tryToPlant(xs: IntProgression, ys: Int, grassMap: Array<List<Int>>) {
        val treeSpecies = 0


        // single "slice" is guaranteed to be 9 blocks wide
        val treePlantable = grassMap.zip(xs.first..xs.last).flatMap { (ys, x) ->
            ys.map { y -> x to y }
        }
        // larger value = more likely to spawn large tree
        // range: [0, 3]
        val woodsWgt = treePlantable.map { (x, y) -> (biomeMap[LandUtil.getBlockAddr(world, x, y)] ?: 0).toUint().and(3) }.average().toDitherredInt(xs.first, ys, 234571)

        val treeToSpawn = when (woodsWgt) { // . - none (0) o - shrub (1) ! - small tree (2) $ - large tree (3)
            // 0: . .
            // 1:  o
            // 2: ! . / . !
            // 3: ! ! /  $
            3 -> {
                val tree = if (nextDouble(xs.first, ys, 541035097) > 0.5)
                    listOf(3)
                else
                    listOf(2, 2)

                if (nextDouble(xs.first, ys, 6431534) < (params as TreegenParams).deepForestTreeProb) tree else listOf()
            }
            2 -> {
                val tree = if (nextDouble(xs.first, ys, 51216464) > 0.5)
                    listOf(0, 2)
                else
                    listOf(2, 0)

                if (nextDouble(xs.first, ys, 125098) < (params as TreegenParams).sparseForestTreeProb) tree else listOf()
            }
            1 -> {
                val tree = listOf(1)

                if (nextDouble(xs.first, ys, 150983) < (params as TreegenParams).plainsShrubProb) tree else listOf()
            }
            else -> listOf()
        }

//        printdbg(this, "Tree to spawn at [${xs.first}..${xs.last}]: $treeToSpawn (woodsWgt=$woodsWgt/$woodsWgtD)")

        when (treeToSpawn.size) {
            2 -> {
                val plot1 = if (treeToSpawn[0] < 3) treePlot1.takeRand(xs.first, ys, 123) else treePlot1[0]
                val plot2 = if (treeToSpawn[1] < 3) treePlot2.takeRand(xs.first, ys, 456) else treePlot2[0]

                // if there is no grass, grassMap[x] is an empty list
                if (treeToSpawn[0] != 0) {
                    grassMap[plot1].let { if (it.isEmpty()) null else it.takeRand(xs.first + plot1, ys, 1234) }?.let {
                        plantTree(xs.first + plot1, it, treeSpecies, 1) // TODO use treeSize from the treeToSpawn
                    }
                }
                if (treeToSpawn[1] != 0) {
                    grassMap[plot2].let { if (it.isEmpty()) null else it.takeRand(xs.first + plot2, ys, 2345) }?.let {
                        plantTree(xs.first + plot2, it, treeSpecies, 1) // TODO use treeSize from the treeToSpawn
                    }
                }
            }
            1 -> {
                val plot1 = if (treeToSpawn[0] < 3) treePlotM.takeRand(xs.first, ys, 3456) else treePlotM[0]

                // if there is no grass, grassMap[x] is an empty list
                if (treeToSpawn[0] != 0) {
                    val treeSize = arrayOf(null, 0, 1, 2)[treeToSpawn[0]]
                    grassMap[plot1].let { if (it.isEmpty()) null else it.takeRand(xs.first + plot1, ys, 4567) }?.let {
                        plantTree(xs.first + plot1, it, treeSpecies, treeSize!!)
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
    private fun plantTree(x: Int, y: Int, type: Int, size: Int) {
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
//                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            val stem = 1
            val bulb1 = 3 + fudgeN(x, y, 4095823)

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
            growCnt = drawBulb(x, y, 3, bulb1, foliage, growCnt)
        }
        else if (size == 1) {
            val heightSum = 5+3+2+1
            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1) {
//                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            // roll for dice until we get a height that fits into the given terrain
            val stem = 7 + fudgeN(x, y, 7548291, 1530948)
            val bulb1 = 4 + fudgeN(x, y, 345098)
            val bulb2 = 3 + fudgeN(x, y, 6093481)
            val bulb3 = 2 + fudgeN(x, y, 5413879)
//            printdbg(this, "Planting tree at $x, $y; params: $stem, $bulb1, $bulb2, $bulb3")

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
            growCnt = drawBulb(x, y, 7, bulb1, foliage, growCnt)
            // bulb 2
            growCnt = drawBulb(x, y, 5, bulb2, foliage, growCnt)
            // bulb 3
            growCnt = drawBulb(x, y, 3, bulb3, foliage, growCnt)
        }
        else if (size == 2) {
            val heightSum = 12+4+3+2+1
            // check for minimum height
            val chkM1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chk0 = (1..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP1 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }
            val chkP2 = (2..heightSum).any { BlockCodex[world.getTileFromTerrain(x, y - it)].isSolid }

            if (chkM1 || chk0 || chkP1 || chkP2) {
//                printdbg(this, "Ceiling not tall enough at $x, $y, aborting")
                return
            }

            // roll for dice until we get a height that fits into the given terrain
            val stem = 15 + fudgeN(x, y, 14509, 509348, 412098357)
            val bulb1 = 5 + fudgeN(x, y, 1254)
            val bulb2 = 4 + fudgeN(x, y, 98134)
            val bulb3 = 3 + fudgeN(x, y, 123098)
            val bulb4 = 2 + fudgeN(x, y, 8712)
//            printdbg(this, "Planting tree at $x, $y; params: $stem, $bulb1, $bulb2, $bulb3, $bulb4")

            // soiling
            val tl1 = world.getTileFromTerrain(x - 1, y)
            val tl2 = world.getTileFromTerrain(x + 1, y)
            if (BlockCodex[tl2].hasTag("INCONSEQUENTIAL")) {
                world.setTileTerrain(x + 1, y, Block.GRASS, true)
                /*
            Case 1
             WW     WW
            GG. -> GGG
            xGx    xDx

             WW     WW
            xG. -> xGG
            xG.    xDG
             */
                if (tl1 == Block.GRASS) {
                    world.setTileTerrain(x, y + 1, Block.DIRT, true)

                    if (BlockCodex[world.getTileFromTerrain(x + 1, y + 1)].hasTag("INCONSEQUENTIAL"))
                    world.setTileTerrain(x + 1, y + 1, Block.DIRT, true)
                }
                /*
            Case 2
             WW     WW
            .G. -> .GG
            xGx    xGx
             */
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
            growCnt = drawBulb(x, y, 10, bulb1, foliage, growCnt)
            // bulb 2
            growCnt = drawBulb(x, y, 8, bulb2, foliage, growCnt)
            // bulb 3
            growCnt = drawBulb(x, y, 6, bulb3, foliage, growCnt)
            // bulb 4
            growCnt = drawBulb(x, y, 4, bulb4, foliage, growCnt)
        }
        else throw IllegalArgumentException("Unknown tree size: $size")
    }

    private fun drawBulb(x: Int, y: Int, width: Int, height: Int, foliage: ItemID, growCnt0: Int): Int {
        var growCnt = growCnt0
        val xStart = x - width / 2 + (1 - (width % 2))
        val xEnd = xStart + width
        var xStart2 = xStart
        var xEnd2 = xEnd

        val r = (XXHash32.hashGeoCoord(x, y) * width * height + growCnt).and(0xffffff) / 16777216f
        r.let {
            if (it < 0.25) xStart2 += 1
            else if (it < 0.5) xEnd2 -= 1
        }
        val xs1 = xStart until xEnd
        val xs2 = xStart2 until xEnd2

        for (i in 0 until height) {
            for (x in if (i == height - 1 && i > 0) xs2 else xs1) {
                val tileHere = world.getTileFromTerrain(x, y - growCnt)
                if (BlockCodex[tileHere].hasTag("INCONSEQUENTIAL"))
                    world.setTileTerrain(x, y - growCnt, foliage, true)
            }
            growCnt += 1
        }
        return growCnt
    }

    /**
     * Rearranges the list such that:
     * `1,2,3,4,5,6,7,8,9`
     * is ordered as:
     * `1,5,9,2,6,3,7,4,8`
     */
    private fun List<IntProgression>.rearrange(): List<IntProgression> {
        val r = ArrayList<IntProgression>()
        val stride = this.size / TerrarumIngame.worldgenThreadExecutor.threadCount

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
    private fun fudgeN(x: Int, y: Int, vararg hs: Int) = hs.sumOf { (nextDouble(x, y, it) * 3).toInt() - 1 }

}

data class TreegenParams(
    val deepForestTreeProb: Double = 0.8,
    val sparseForestTreeProb: Double = 0.5,
    val plainsShrubProb: Double = 0.25,
)