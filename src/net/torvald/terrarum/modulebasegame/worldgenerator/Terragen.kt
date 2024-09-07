package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen.getClampedHeight
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-07-23.
 */
class Terragen(world: GameWorld, isFinal: Boolean , val highlandLowlandSelectCache: ModuleCache, seed: Long, params: Any) : Gen(world, isFinal, seed, params) {

    private val dirtStoneDitherSize = 3 // actual dither size will be double of this value
    private val stoneSlateDitherSize = 4

    override fun getDone(loadscreen: LoadScreenBase?) {
        loadscreen?.let {
            it.stageValue += 1
            it.progress.set(0L)
        }

        Worldgen.threadExecutor.renew()
        submitJob(loadscreen)
        Worldgen.threadExecutor.join()
    }


    private val groundDepthBlock = listOf(
            Block.AIR, Block.DIRT, Block.STONE, Block.STONE_SLATE
    )

    private fun Double.tiered(tiers: List<Double>): Int {
        tiers.reversed().forEachIndexed { index, it ->
            if (this >= it) return (tiers.lastIndex - index) // why??
        }
        return tiers.lastIndex
    }

    private val terragenYscaling = (world.getClampedHeight() / 2400.0).pow(0.75)
    private val terragenTiers = (params as TerragenParams).terragenTiers.map { it * terragenYscaling } // pow 1.0 for 1-to-1 scaling; 0.75 is used to make deep-rock layers actually deep for huge world size

    //private fun draw(x: Int, y: Int, width: Int, height: Int, noiseValue: List<Double>, world: GameWorld) {
    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        for (x in xStart until xStart + CHUNK_W) {
            val st = (x.toDouble() / world.width) * TWO_PI

            var dirtStoneTransition = -1
            var stoneSlateTransition = -1

            for (y in yStart until yStart + CHUNK_H) {
                val sx = sin(st) * soff + soff // plus sampleOffset to make only
                val sz = cos(st) * soff + soff // positive points are to be sampled
                val sy = Worldgen.getSY(y)
                // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
                val noiseValue = noises.map { it.get(sx, sy, sz) }

                val terr = noiseValue[0].tiered(terragenTiers)


                // disable the marker if relativeY=0 already has rock
                if (y == yStart && terr == 2)
                    dirtStoneTransition = -2
                else if (y == yStart && terr == 3)
                    stoneSlateTransition = -2
                // mark off the position where the transition occurred
                else {
                    if (dirtStoneTransition == -1 && terr == 2)
                        dirtStoneTransition = y
                    if (stoneSlateTransition == -1 && terr == 3)
                        stoneSlateTransition = y
                }

                val isMarble = noiseValue[1] > 0.5

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

        val groundClamp = ModuleClamp().also {
            it.setRange(0.0, 100.0)
            it.setSource(highlandLowlandSelectCache)
        }

        val groundScaling = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(groundClamp)
        }

        val marblerng = HQRNG(seed) // this must be here: every slice must get identical series of random numbers

        return listOf(
            Joise(groundScaling),

            Joise(generateRockLayer(groundScaling, seed, params, (0..7).map {
                thicknesses[it] + marblerng.nextTriangularBal() * 0.006 to (1.04 * params.terragenTiers[3] * terragenYscaling) + it * 0.18 + marblerng.nextTriangularBal() * 0.09
            })),
        )
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

interface TerragenParams {
    val terragenTiers: List<Double>
    val featureSize: Double
    val lowlandScaleOffset: Double // linearly alters the height
    val highlandScaleOffset: Double // linearly alters the height
    val mountainScaleOffset: Double // linearly alters the height
    val mountainDisturbance: Double // greater = more distortion, overhangs
    val caveShapeFreq: Double //adjust the "density" of the caves
    val caveAttenuateScale: Double // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    val caveAttenuateBias: Double // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    val caveAttenuateScale1: Double // used with the caveAttenuateBias, controls the "concentration" of the cave gen
    val caveAttenuateBias1: Double // 1.0: flattens the gradient (deep voids are less tend to be larger). Also controls the distribution of ores. Equation: x^(log(bias) / log(0.5))
    val caveSelectThre: Double // also adjust this if you've touched the bias value. Number can be greater than 1.0
    val caveBlockageFractalFreq: Double
    val caveBlockageSelectThre: Double // adjust cave closing-up strength. Lower = more closing
    val rockBandCutoffFreq: Double

    val lavaShapeFreg: Double }

data class TerragenParamsAlpha1(
    override val terragenTiers: List<Double> = listOf(.0, .5, 1.0, 2.5, 11.0 * 9999),

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
) : TerragenParams

data class TerragenParamsAlpha2(
    override val terragenTiers: List<Double> = listOf(.0, .5, 1.5, 3.75, 11.0 * 9999),

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

) : TerragenParams