package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-07-23.
 */
class Terragen(world: GameWorld, isFinal: Boolean, val groundScalingCached: ModuleCache, seed: Long, params: Any) : Gen(world, isFinal, seed, params) {

    private val isAlpha2 = ((params as TerragenParams).versionSince >= 0x0000_000004_000004)

    override fun getDone(loadscreen: LoadScreenBase?) {
        loadscreen?.let {
            it.stageValue += 1
            it.progress.set(0L)
        }

        Worldgen.threadExecutor.renew()
        submitJob(loadscreen)
        Worldgen.threadExecutor.join()
    }


    private fun Double.tiered(tiers: List<Double>): Int {
        tiers.reversed().forEachIndexed { index, it ->
            if (this >= it) return (tiers.lastIndex - index) // why??
        }
        return tiers.lastIndex
    }

    private val terragenYscaling =
        if (isAlpha2)
            1.0
        else
            (world.height / 2400.0).pow(0.75)

    //private fun draw(x: Int, y: Int, width: Int, height: Int, noiseValue: List<Double>, world: GameWorld) {
    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        val strataMode = if (!isAlpha2)
            0
        else
            1// TODO

        val strata = (params as TerragenParams).getStrataForMode(seed, strataMode)
        val groundDepthBlock = strata.map { it.tiles }
        val terragenTiers = strata.map { it.yheight * terragenYscaling }

        for (x in xStart until xStart + CHUNK_W) {
            val st = (x.toDouble() / world.width) * TWO_PI

            for (y in yStart until yStart + CHUNK_H) {
                val sx = sin(st) * soff + soff // plus sampleOffset to make only
                val sz = cos(st) * soff + soff // positive points are to be sampled
                val sy = Worldgen.getSY(y)
                // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
                val noiseValue = noises.map { it.get(sx, sy, sz) }

                val terr = noiseValue[0].tiered(terragenTiers)

                val isMarble = if (!isAlpha2) noiseValue[1] > 0.5 else false

                val wallBlock = if (isMarble) Block.STONE_MARBLE else groundDepthBlock[terr]
                val terrBlock = if (isMarble) Block.STONE_MARBLE else wallBlock

                world.setTileTerrain(x, y, terrBlock, true)
                world.setTileWall(x, y, wallBlock, true)
            }


            // dither shits
            /*
        #
        # - dirt-to-cobble transition, height = dirtStoneDitherSize
        #
        %
        % - cobble-to-rock transition, height = dirtStoneDitherSize
        %
        * - where the stone layer actually begins
         */
            /*if (dirtStoneTransition >= 0) {
                for (pos in 0 until dirtStoneDitherSize * 2) {
                    val y = pos + dirtStoneTransition - (dirtStoneDitherSize * 2) + 1
                    if (y >= world.height) break
                    val hash = XXHash32.hashGeoCoord(x, y).and(0xFFFFFF) / 16777216.0
//            val fore = world.getTileFromTerrain(x, y)
//            val back = world.getTileFromWall(x, y)
                    val newTile = if (pos < dirtStoneDitherSize)
                        if (hash < pos.toDouble() / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.DIRT
                    else // don't +1 to pos.toDouble(); I've suffered
                        if (hash >= (pos.toDouble() - dirtStoneDitherSize) / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.STONE

//            if (fore != Block.AIR)
                    if (y in yStart until yStart + CHUNK_H) {
                        world.setTileTerrain(x, y, newTile, true)
                        world.setTileWall(x, y, newTile, true)
                    }
                }
            }*/

            /*
        #
        # - stone-to-slate transition, height = stoneSlateDitherSize
        #
         */
            /*if (stoneSlateTransition >= 0) {
                for (pos in 0 until stoneSlateDitherSize) {
                    val y = pos + stoneSlateTransition - stoneSlateDitherSize + 1
                    if (y >= world.height) break
                    val hash = XXHash32.hashGeoCoord(x, y).and(0xFFFFFF) / 16777216.0
//            val fore = world.getTileFromTerrain(x, y)
//            val back = world.getTileFromWall(x, y)
                    val newTile = if (hash < pos.toDouble() / stoneSlateDitherSize) Block.STONE_SLATE else Block.STONE

                    if (y in yStart until yStart + CHUNK_H) {
//            if (fore != Block.AIR)
                        world.setTileTerrain(x, y, newTile, true)
                        world.setTileWall(x, y, newTile, true)
                    }
                }
            }*/
        }
    }

    private val thicknesses = listOf(0.016, 0.021, 0.029, 0.036, 0.036, 0.029, 0.021, 0.016)

    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        val params = params as TerragenParams

        // this noise tree WILL generate noise value greater than 1.0
        // they should be treated properly when you actually generate the world out of the noisemap
        // for the visualisation, no treatment will be done in this demo app.

        return if (!isAlpha2) {
            val marblerng = HQRNG(seed) // this must be here: every slice must get identical series of random numbers
            listOf(
                Joise(groundScalingCached),
                Joise(generateRockLayer(groundScalingCached, seed, params, (0..7).map {
                    thicknesses[it] + marblerng.nextTriangularBal() * 0.006 to (1.04 * params.strata[3].yheight.h * terragenYscaling) + it * 0.18 + marblerng.nextTriangularBal() * 0.09
                })),
            )
        }
        else
            listOf(Joise(groundScalingCached))
    }

    private fun generateRockLayer(ground: Module, seed: Long, params: TerragenParams, thicknessAndRange: List<Pair<Double, Double>>): Module {

        val occlusion = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(params.rockBandCutoffFreq / params.featureSize) // adjust the "density" of the veins
            it.seed = seed shake 0x41A2B1E5
        }

        val occlusionScale = ModuleScaleDomain().also {
            it.setScaleX(0.5)
            it.setScaleZ(0.5)
            it.setSource(occlusion)
        }

        val occlusionBinary = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(occlusionScale)
            it.setThreshold(1.1)
            it.setFalloff(0.0)
        }

        val occlusionCache = ModuleCache().also {
            it.setSource(occlusionBinary)
        }

        val bands = thicknessAndRange.map { (thickness, rangeStart) ->
            val thresholdLow = ModuleSelect().also {
                it.setLowSource(0.0)
                it.setHighSource(1.0)
                it.setControlSource(ground)
                it.setThreshold(rangeStart)
                it.setFalloff(0.0)
            }

            val thresholdHigh = ModuleSelect().also {
                it.setLowSource(1.0)
                it.setHighSource(0.0)
                it.setControlSource(ground)
                it.setThreshold(rangeStart + thickness)
                it.setFalloff(0.0)
            }

            ModuleCombiner().also {
                it.setSource(0, thresholdLow)
                it.setSource(1, thresholdHigh)
                it.setSource(2, occlusionCache)
                it.setType(ModuleCombiner.CombinerType.MULT)
            }
        }


        val combinedBands = ModuleCombiner().also {
            bands.forEachIndexed { index, module ->
                it.setSource(index, module)
            }
            it.setType(ModuleCombiner.CombinerType.ADD)
        }

        return combinedBands
    }
}

abstract class TerragenParams {
    val LANDBLOCK_VAR_COUNTS = 16

    abstract val versionSince: Long
    abstract val strata: List<Stratum>
    abstract val featureSize: Double
    abstract val lowlandScaleOffset: Double // linearly alters the height
    abstract val highlandScaleOffset: Double // linearly alters the height
    abstract val mountainScaleOffset: Double // linearly alters the height
    abstract val mountainDisturbance: Double // greater = more distortion, overhangs
    abstract val caveShapeFreq: Double //adjust the "density" of the caves
    abstract val caveAttenuateScale: Double // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    abstract val caveAttenuateBias: Double // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    abstract val caveAttenuateScale1: Double // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    abstract val caveAttenuateBias1: Double // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    abstract val caveSelectThre: Double // also adjust this if you've touched the bias value. Number can be greater than 1.0
    abstract val caveBlockageFractalFreq: Double
    abstract val caveBlockageSelectThre: Double // adjust cave closing-up strength. Lower = more closing
    abstract val rockBandCutoffFreq: Double
    abstract val lavaShapeFreg: Double
    abstract val landBlockScale: Double

    private val strataCache = HashMap<Long, List<StratumObj>?>()

    private fun generateStrataCache(generatorSeed: Long, mode: Int): Long {
        val index = generatorSeed shake mode.toLong()

        val rng = HQRNG(index)

        val tilebuf = Array(2) { "" }
        fun shiftTilebuf(item: String) {
            tilebuf[0] = tilebuf[1]
            tilebuf[1] = item
        }

        strata.map {
            // 0. randomiser gets two-element buffer
            // 1. randomiser consults the buffer and removes matching elements from the random pool
            // 2. if the pool is empty, pool is reset with the original candidates
            // 3. randomiser makes random choices
            // 4. if the size of the ORIGINAL candidates > 1, the chosen element gets queued into the buffer

            var tilePool = it.tiles.toMutableList() // toList makes copies, asList keeps original pointers
            tilebuf.forEach { tilePool.remove(it) }
            if (tilePool.isEmpty()) tilePool = it.tiles.toMutableList()
            val selectedTile = tilePool[rng.nextInt(tilePool.size)]
            if (it.tiles.size > 1) shiftTilebuf(selectedTile)

            StratumObj(
                (if (it.yheight.v > 1.0 / 1024.0)
                    rng.nextDouble(it.yheight.h - it.yheight.v, it.yheight.h + it.yheight.v)
                else
                    it.yheight.h),
                selectedTile
            )
        }.let {
            strataCache[index] = it
        }

        return index
    }

    fun pregenerateStrataCache(generatorSeed: Long) {
        for (i in 0 until LANDBLOCK_VAR_COUNTS) {
            generateStrataCache(generatorSeed, i)
        }
    }

    fun getStrataForMode(generatorSeed: Long, mode: Int): List<StratumObj> {
        return strataCache[generatorSeed shake mode.toLong()]!!
    }
}

/**
 * @param h: STARTING height of the strata relative to the ground select gradient
 * @param v: linear ± to the `h`
 */
data class Hv(val h: Double, val v: Double) { init {
    if (h.isNaN() || h.isInfinite() || h < 0.0) throw IllegalArgumentException("h-value must be zero or positive (got $h)")
    if (v.isNaN() || v.isInfinite() || v < 0.0) throw IllegalArgumentException("v-value must be zero or positive (got $v)")
} }
class Stratum(val yheight: Hv, vararg val tiles: ItemID)
class StratumObj(val yheight: Double, val tiles: ItemID)

data class TerragenParamsAlpha1(
    override val versionSince: Long = 0L,

    // 0. randomiser gets two-element buffer
    // 1. randomiser consults the buffer and removes matching elements from the random pool
    // 2. if the pool is empty, pool is reset with the original candidates
    // 3. randomiser makes random choices
    // 4. if the size of the ORIGINAL candidates > 1, the chosen element gets queued into the buffer
    override val strata: List<Stratum> = listOf(
        Stratum(Hv(.0, .0), Block.AIR),
        Stratum(Hv(.5, .0), Block.DIRT),
        Stratum(Hv(1.0, .0), Block.STONE),
        Stratum(Hv(2.5, .0), Block.STONE_GABBRO),
    ),

    override val featureSize: Double = 333.0,
    override val lowlandScaleOffset: Double = -0.65, // linearly alters the height
    override val highlandScaleOffset: Double = -0.2, // linearly alters the height
    override val mountainScaleOffset: Double = -0.1, // linearly alters the height
    override val mountainDisturbance: Double = 0.7, // greater = more distortion, overhangs

    override val caveShapeFreq: Double = 4.0, //adjust the "density" of the caves
    override val caveAttenuateScale: Double = 1.0, // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    override val caveAttenuateBias: Double = 0.90, // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    override val caveAttenuateScale1: Double = 1.0, // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    override val caveAttenuateBias1: Double = 0.90, // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    override val caveSelectThre: Double = 0.918, // also adjust this if you've touched the bias value. Number can be greater than 1.0
    override val caveBlockageFractalFreq: Double = 8.88,
    override val caveBlockageSelectThre: Double = 1.40, // adjust cave closing-up strength. Lower = more closing

    override val rockBandCutoffFreq: Double = 4.0,

    override val lavaShapeFreg: Double = 0.03,

    override val landBlockScale: Double = 1.0,

    ) : TerragenParams()

data class TerragenParamsAlpha2(
    override val versionSince: Long = 0x0000_000004_000004,

    override val strata: List<Stratum> = listOf(
        Stratum(Hv(.0, .0), Block.AIR),
        Stratum(Hv(.5, .0), Block.DIRT),
        Stratum(Hv(1.0, 0.01), Block.STONE, Block.STONE_LIMESTONE), // 0.01 represents less-eroded faults
        Stratum(Hv(1.1, 0.03), Block.STONE, Block.SANDSTONE, Block.STONE_LIMESTONE),
        Stratum(Hv(1.2, 0.03), Block.STONE, Block.SANDSTONE, Block.STONE_LIMESTONE),
        Stratum(Hv(1.3, 0.03), Block.SANDSTONE, Block.STONE_LIMESTONE, Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE),
        Stratum(Hv(1.4, 0.03), Block.STONE_LIMESTONE, Block.STONE_PLAGIOCLASE, Block.SANDSTONE),
        Stratum(Hv(1.5, 0.03), Block.STONE, Block.STONE_MICROCLINE, Block.STONE_LIMESTONE),

        Stratum(Hv(2.0, 0.3), Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE),
        Stratum(Hv(3.0, 0.3), Block.STONE, Block.STONE_MICROCLINE, Block.STONE_LIMESTONE),
        Stratum(Hv(4.0, 0.3), Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE),
        Stratum(Hv(5.0, 0.3), Block.STONE, Block.STONE_MICROCLINE, Block.STONE_LIMESTONE),
        Stratum(Hv(6.0, 0.3), Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE),

        Stratum(Hv(6.1, 0.03), Block.STONE_MARBLE, Block.STONE_SLATE),
        Stratum(Hv(6.2, 0.03), Block.STONE, Block.STONE_PLAGIOCLASE, Block.STONE_MICROCLINE),
        Stratum(Hv(6.3, 0.03), Block.STONE_MARBLE, Block.STONE_SLATE),
        Stratum(Hv(6.4, 0.03), Block.STONE, Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE, Block.STONE_MICROCLINE),
        Stratum(Hv(6.5, 0.03), Block.STONE_MARBLE, Block.STONE_SLATE),
        Stratum(Hv(6.6, 0.03), Block.STONE, Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE, Block.STONE_MICROCLINE),
        Stratum(Hv(6.7, 0.03), Block.STONE_MARBLE, Block.STONE_SLATE),

        Stratum(Hv(6.9, 0.06), Block.STONE, Block.STONE_ORTHOCLASE, Block.STONE_PLAGIOCLASE, Block.STONE_MICROCLINE),

        Stratum(Hv(7.0, 0.3), Block.STONE, Block.STONE_MICROCLINE),
        Stratum(Hv(8.0, 0.3), Block.STONE_BASALT),
        Stratum(Hv(9.0, 0.3), Block.STONE),
        Stratum(Hv(10.0, 0.3), Block.STONE, Block.STONE_GABBRO),
        Stratum(Hv(11.0, 0.3), Block.STONE_GABBRO),
    ),

    override val featureSize: Double = 333.0,
    override val lowlandScaleOffset: Double = -0.65, // linearly alters the height
    override val highlandScaleOffset: Double = -0.2, // linearly alters the height
    override val mountainScaleOffset: Double = -0.1, // linearly alters the height
    override val mountainDisturbance: Double = 0.7, // greater = more distortion, overhangs

    override val caveShapeFreq: Double = 4.0, //adjust the "density" of the caves
    override val caveAttenuateScale: Double = 0.94, // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    override val caveAttenuateBias: Double = 0.95, // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    override val caveAttenuateScale1: Double = 0.90, // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    override val caveAttenuateBias1: Double = 0.90, // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    override val caveSelectThre: Double = 0.915, // also adjust this if you've touched the bias value. Number can be greater than 1.0
    override val caveBlockageFractalFreq: Double = 8.88,
    override val caveBlockageSelectThre: Double = 1.40, // adjust cave closing-up strength. Lower = more closing

    override val rockBandCutoffFreq: Double = 4.0,

    override val lavaShapeFreg: Double = 0.03,

    override val landBlockScale: Double = 0.5,

    ) : TerragenParams()