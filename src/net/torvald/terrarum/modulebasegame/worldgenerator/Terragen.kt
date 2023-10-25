package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.XXHash32
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-07-23.
 */
class Terragen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {

    companion object {
        const val YHEIGHT_MAGIC = 2800.0 / 3.0
        const val YHEIGHT_DIVISOR = 2.0 / 7.0
    }

    private val threadExecutor = TerrarumIngame.worldgenThreadExecutor

    private val genSlices = max(threadExecutor.threadCount, world.width / 8)


    private val dirtStoneDitherSize = 3 // actual dither size will be double of this value
    private val stoneSlateDitherSize = 4

    override fun getDone() {
        threadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).mapIndexed { i, xs ->
            threadExecutor.submit {
                val localJoise = getGenerator(seed, params as TerragenParams)
                for (x in xs) {
                    val sampleTheta = (x.toDouble() / world.width) * TWO_PI
                    val sampleOffset = world.width / 8.0
                    draw(x, localJoise, sampleTheta, sampleOffset)
                }
            }
        }

        threadExecutor.join()

        printdbg(this, "Waking up Worldgen")
    }


    private val groundDepthBlock = listOf(
            Block.AIR, Block.DIRT, Block.STONE, Block.STONE_SLATE
    )

    private fun Double.tiered(vararg tiers: Double): Int {
        tiers.reversed().forEachIndexed { index, it ->
            if (this >= it) return (tiers.lastIndex - index) // why??
        }
        return tiers.lastIndex
    }

    //private fun draw(x: Int, y: Int, width: Int, height: Int, noiseValue: List<Double>, world: GameWorld) {
    private fun draw(x: Int, noises: List<Joise>, st: Double, soff: Double) {
        var dirtStoneTransition = 0
        var stoneSlateTransition = 0

        for (y in 0 until world.height) {
            val sx = sin(st) * soff + soff // plus sampleOffset to make only
            val sz = cos(st) * soff + soff // positive points are to be sampled
            val sy = y - (world.height - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant
            // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
            val noiseValue = noises.map { it.get(sx, sy, sz) }

            val terr = noiseValue[0].tiered(.0, .5, .88, 1.88)
            val cave = if (noiseValue[1] < 0.5) 0 else 1

            // mark off the position where the transition occurred
            if (dirtStoneTransition == 0 && terr == 2)
                dirtStoneTransition = y
            if (stoneSlateTransition == 0 && terr == 3)
                stoneSlateTransition = y

            val wallBlock = groundDepthBlock[terr]
            val terrBlock = if (cave == 0) Block.AIR else wallBlock

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
        for (pos in 0 until dirtStoneDitherSize * 2) {
            val y = pos + dirtStoneTransition - (dirtStoneDitherSize * 2) + 1
            if (y >= world.height) break
            val hash = XXHash32.hashGeoCoord(x, y).and(0x7FFFFFFF) / 2147483647.0
            val fore = world.getTileFromTerrain(x, y)
            val back = world.getTileFromWall(x, y)
            val newTile = if (pos < dirtStoneDitherSize)
                if (hash < pos.toDouble() / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.DIRT
            else // don't +1 to pos.toDouble(); I've suffered
                if (hash >= (pos.toDouble() - dirtStoneDitherSize) / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.STONE

            if (fore != Block.AIR)
                world.setTileTerrain(x, y, newTile, true)

            world.setTileWall(x, y, newTile, true)
        }

        /*
        #
        # - stone-to-slate transition, height = stoneSlateDitherSize
        #
         */
        for (pos in 0 until stoneSlateDitherSize) {
            val y = pos + stoneSlateTransition - stoneSlateDitherSize + 1
            if (y >= world.height) break
            val hash = XXHash32.hashGeoCoord(x, y).and(0x7FFFFFFF) / 2147483647.0
            val fore = world.getTileFromTerrain(x, y)
            val back = world.getTileFromWall(x, y)
            val newTile = if (hash < pos.toDouble() / stoneSlateDitherSize) Block.STONE_SLATE else Block.STONE

            if (fore != Block.AIR)
                world.setTileTerrain(x, y, newTile, true)

            world.setTileWall(x, y, newTile, true)
        }
    }


    private fun getGenerator(seed: Long, params: TerragenParams): List<Joise> {
        val lowlandMagic: Long = 0x41A21A114DBE56 // Maria Lindberg
        val highlandMagic: Long = 0x0114E091      // Olive Oyl
        val mountainMagic: Long = 0x115AA4DE2504  // Lisa Anderson
        val selectionMagic: Long = 0x41E10D9B100  // Melody Blue

        val caveMagic: Long = 0x00215741CDF // Urist McDF
        val cavePerturbMagic: Long = 0xA2410C // Armok
        val caveBlockageMagic: Long = 0xD15A57E5 // Disaster


        val groundGradient = ModuleGradient().also {
            it.setGradient(0.0, 0.0, 0.0, 1.0)
        }

        /* lowlands */

        val lowlandShapeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.BILLOW)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(0.25)
            it.seed = seed shake lowlandMagic
        }

        val lowlandScale = ModuleScaleOffset().also {
            it.setSource(lowlandShapeFractal)
            it.setScale(0.22)
            it.setOffset(params.lowlandScaleOffset) // linearly alters the height
        }

        val lowlandYScale = ModuleScaleDomain().also {
            it.setSource(lowlandScale)
            it.setScaleY(0.02) // greater = more distortion, overhangs
        }

        val lowlandTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(lowlandYScale)
        }

        /* highlands */

        val highlandShapeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(4)
            it.setFrequency(2.0)
            it.seed = seed shake highlandMagic
        }

        val highlandScale = ModuleScaleOffset().also {
            it.setSource(highlandShapeFractal)
            it.setScale(0.5)
            it.setOffset(params.highlandScaleOffset) // linearly alters the height
        }

        val highlandYScale = ModuleScaleDomain().also {
            it.setSource(highlandScale)
            it.setScaleY(0.14) // greater = more distortion, overhangs
        }

        val highlandTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(highlandYScale)
        }

        /* mountains */

        val mountainShapeFractal = ModuleFractal().also {
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(8)
            it.setFrequency(1.0)
            it.seed = seed shake mountainMagic
        }

        val mountainScale = ModuleScaleOffset().also {
            it.setSource(mountainShapeFractal)
            it.setScale(1.0)
            it.setOffset(params.mountainScaleOffset) // linearly alters the height
        }

        val mountainYScale = ModuleScaleDomain().also {
            it.setSource(mountainScale)
            it.setScaleY(params.mountainDisturbance) // greater = more distortion, overhangs
        }

        val mountainTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(mountainYScale)
        }

        /* selection */

        val terrainTypeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(3)
            it.setFrequency(0.125)
            it.seed = seed shake selectionMagic
        }

        val terrainScaleOffset = ModuleScaleOffset().also {
            it.setSource(terrainTypeFractal)
            it.setOffset(0.5)
            it.setScale(0.666666) // greater = more dynamic terrain
        }

        val terrainTypeYScale = ModuleScaleDomain().also {
            it.setSource(terrainScaleOffset)
            it.setScaleY(0.0)
        }

        val terrainTypeCache = ModuleCache().also {
            it.setSource(terrainTypeYScale)
        }

        val highlandMountainSelect = ModuleSelect().also {
            it.setLowSource(highlandTerrain)
            it.setHighSource(mountainTerrain)
            it.setControlSource(terrainTypeCache)
            it.setThreshold(0.55)
            it.setFalloff(0.2)
        }

        val highlandLowlandSelect = ModuleSelect().also {
            it.setLowSource(lowlandTerrain)
            it.setHighSource(highlandMountainSelect)
            it.setControlSource(terrainTypeCache)
            it.setThreshold(0.25)
            it.setFalloff(0.15)
        }

        val highlandLowlandSelectCache = ModuleCache().also {
            it.setSource(highlandLowlandSelect)
        }

        val groundSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setThreshold(0.5)
            it.setControlSource(highlandLowlandSelectCache)
        }

        val groundSelect2 = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setThreshold(0.8)
            it.setControlSource(highlandLowlandSelectCache)
        }

        /* caves */

        val caveShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(1)
            it.setFrequency(params.caveShapeFreq) // adjust the "density" of the caves
            it.seed = seed shake caveMagic
        }

        val caveAttenuateBias = ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        }

        val caveShapeAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, caveShape)
            it.setSource(1, caveAttenuateBias)
        }

        val cavePerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(params.caveShapeFreq * 3.0 / 4.0)
            it.seed = seed shake cavePerturbMagic
        }

        val cavePerturbScale = ModuleScaleOffset().also {
            it.setSource(cavePerturbFractal)
            it.setScale(0.45)
            it.setOffset(0.0)
        }

        val cavePerturb = ModuleTranslateDomain().also {
            it.setSource(caveShapeAttenuate)
            it.setAxisXSource(cavePerturbScale)
        }

        val caveSelect = ModuleSelect().also {
            it.setLowSource(1.0)
            it.setHighSource(0.0)
            it.setControlSource(cavePerturb)
            it.setThreshold(params.caveSelectThre) // also adjust this if you've touched the bias value. Number can be greater than 1.0
            it.setFalloff(0.0)
        }

        val caveBlockageFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(params.caveBlockageFractalFreq) // same as caveShape frequency?
            it.seed = seed shake caveBlockageMagic
        }

        // will only close-up deeper caves. Shallow caves will be less likely to be closed up
        val caveBlockageAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, caveBlockageFractal)
            it.setSource(1, caveAttenuateBias)
        }

        val caveBlockageSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(caveBlockageAttenuate)
            it.setThreshold(params.caveBlockageSelectThre) // adjust cave cloing-up strength. Larger = more closing
            it.setFalloff(0.0)
        }

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.ADD)
            it.setSource(0, caveSelect)
            it.setSource(1, caveBlockageSelect)
        }

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

        val caveClamp = ModuleClamp().also {
            it.setRange(0.0, 1.0)
            it.setSource(caveInMix)
        }

        val caveScaling = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveClamp)
        }

        //return Joise(caveInMix)
        return listOf(
                Joise(groundScaling),
                Joise(caveScaling)
        )
    }
}

data class TerragenParams(
        val featureSize: Double = 333.0,
        val lowlandScaleOffset: Double = -0.65, // linearly alters the height
        val highlandScaleOffset: Double = -0.2, // linearly alters the height
        val mountainScaleOffset: Double = -0.1, // linearly alters the height
        val mountainDisturbance: Double = 0.7, // greater = more distortion, overhangs
        val caveShapeFreq: Double = 4.0, //adjust the "density" of the caves
        val caveAttenuateBias: Double = 0.90, // adjust the "concentration" of the cave gen. Lower = larger voids
        val caveSelectThre: Double = 0.918, // also adjust this if you've touched the bias value. Number can be greater than 1.0
        val caveBlockageFractalFreq: Double = 8.88,
        val caveBlockageSelectThre: Double = 1.40 // adjust cave cloing-up strength. Larger = more closing
)