package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.toInt
import java.util.concurrent.Future
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-07-23.
 */
class Terragen(world: GameWorld, seed: Long, params: Any) : Gen(world, seed, params) {

    private val genSlices = maxOf(ThreadExecutor.threadCount, world.width / 8)

    private val YHEIGHT_MAGIC = 2800.0 / 3.0
    private val YHEIGHT_DIVISOR = 2.0 / 7.0

    override fun getDone() {
        ThreadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).mapIndexed { i, xs ->
            ThreadExecutor.submit {
                val localJoise = getGenerator(seed, params as TerragenParams)
                val localLock = java.lang.Object() // in an attempt to fix the "premature exit" issue of a thread run
                synchronized(localLock) {          // also see: https://stackoverflow.com/questions/28818494/threads-stopping-prematurely-for-certain-values
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
        }

        ThreadExecutor.join()

        printdbg(this, "Waking up Worldgen")
    }


    private val groundDepthBlock = listOf(
            Block.AIR, Block.DIRT, Block.STONE
    )

    private fun draw(x: Int, y: Int, noiseValue: List<Double>, world: GameWorld) {
        fun Double.tiered(vararg tiers: Double): Int {
            tiers.reversed().forEachIndexed { index, it ->
                if (this >= it) return (tiers.lastIndex - index) // why??
            }
            return tiers.lastIndex
        }

        val terr = noiseValue[0].tiered(.0, .5, .88)
        val cave = if (noiseValue[1] < 0.5) 0 else 1

        val wallBlock = groundDepthBlock[terr]
        val terrBlock = if (cave == 0) Block.AIR else wallBlock //wallBlock * cave // AIR is always zero, this is the standard

        world.setTileTerrain(x, y, terrBlock)
        world.setTileWall(x, y, wallBlock)
    }


    private fun getGenerator(seed: Long, params: TerragenParams): List<Joise> {
        val lowlandMagic: Long = 0x41A21A114DBE56 // Maria Lindberg
        val highlandMagic: Long = 0x0114E091      // Olive Oyl
        val mountainMagic: Long = 0x115AA4DE2504  // Lisa Anderson
        val selectionMagic: Long = 0x41E10D9B100  // Melody Blue

        val caveMagic: Long = 0x00215741CDF // Urist McDF
        val cavePerturbMagic: Long = 0xA2410C // Armok
        val caveBlockageMagic: Long = 0xD15A57E5 // Disaster


        val groundGradient = ModuleGradient()
        groundGradient.setGradient(0.0, 0.0, 0.0, 1.0)

        /* lowlands */

        val lowlandShapeFractal = ModuleFractal()
        lowlandShapeFractal.setType(ModuleFractal.FractalType.BILLOW)
        lowlandShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        lowlandShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        lowlandShapeFractal.setNumOctaves(2)
        lowlandShapeFractal.setFrequency(0.25)
        lowlandShapeFractal.seed = seed shake lowlandMagic

        val lowlandAutoCorrect = ModuleAutoCorrect()
        lowlandAutoCorrect.setSource(lowlandShapeFractal)
        lowlandAutoCorrect.setLow(0.0)
        lowlandAutoCorrect.setHigh(1.0)

        val lowlandScale = ModuleScaleOffset()
        lowlandScale.setScale(0.125)
        lowlandScale.setOffset(params.lowlandScaleOffset) // TODO linearly alters the height

        val lowlandYScale = ModuleScaleDomain()
        lowlandYScale.setSource(lowlandScale)
        lowlandYScale.setScaleY(0.02) // greater = more distortion, overhangs

        val lowlandTerrain = ModuleTranslateDomain()
        lowlandTerrain.setSource(groundGradient)
        lowlandTerrain.setAxisYSource(lowlandYScale)

        /* highlands */

        val highlandShapeFractal = ModuleFractal()
        highlandShapeFractal.setType(ModuleFractal.FractalType.FBM)
        highlandShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        highlandShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        highlandShapeFractal.setNumOctaves(4)
        highlandShapeFractal.setFrequency(2.0)
        highlandShapeFractal.seed = seed shake highlandMagic

        val highlandAutocorrect = ModuleAutoCorrect()
        highlandAutocorrect.setSource(highlandShapeFractal)
        highlandAutocorrect.setLow(-1.0)
        highlandAutocorrect.setHigh(1.0)

        val highlandScale = ModuleScaleOffset()
        highlandScale.setSource(highlandAutocorrect)
        highlandScale.setScale(0.25)
        highlandScale.setOffset(params.highlandScaleOffset) // TODO linearly alters the height

        val highlandYScale = ModuleScaleDomain()
        highlandYScale.setSource(highlandScale)
        highlandYScale.setScaleY(0.14) // greater = more distortion, overhangs

        val highlandTerrain = ModuleTranslateDomain()
        highlandTerrain.setSource(groundGradient)
        highlandTerrain.setAxisYSource(highlandYScale)

        /* mountains */

        val mountainShapeFractal = ModuleFractal()
        mountainShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        mountainShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        mountainShapeFractal.setNumOctaves(8)
        mountainShapeFractal.setFrequency(1.0)
        mountainShapeFractal.seed = seed shake mountainMagic

        val mountainAutocorrect = ModuleAutoCorrect()
        mountainAutocorrect.setSource(mountainShapeFractal)
        mountainAutocorrect.setLow(-1.0)
        mountainAutocorrect.setHigh(1.0)

        val mountainScale = ModuleScaleOffset()
        mountainScale.setSource(mountainAutocorrect)
        mountainScale.setScale(0.45)
        mountainScale.setOffset(params.mountainScaleOffset) // TODO linearly alters the height

        val mountainYScale = ModuleScaleDomain()
        mountainYScale.setSource(mountainScale)
        mountainYScale.setScaleY(params.mountainDisturbance) // greater = more distortion, overhangs

        val mountainTerrain = ModuleTranslateDomain()
        mountainTerrain.setSource(groundGradient)
        mountainTerrain.setAxisYSource(mountainYScale)

        /* selection */

        val terrainTypeFractal = ModuleFractal()
        terrainTypeFractal.setType(ModuleFractal.FractalType.FBM)
        terrainTypeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        terrainTypeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        terrainTypeFractal.setNumOctaves(3)
        terrainTypeFractal.setFrequency(0.125)
        terrainTypeFractal.seed = seed shake selectionMagic

        val terrainAutocorrect = ModuleAutoCorrect()
        terrainAutocorrect.setSource(terrainTypeFractal)
        terrainAutocorrect.setLow(0.0)
        terrainAutocorrect.setHigh(1.0)

        val terrainTypeYScale = ModuleScaleDomain()
        terrainTypeYScale.setSource(terrainAutocorrect)
        terrainTypeYScale.setScaleY(0.0)

        val terrainTypeCache = ModuleCache()
        terrainTypeCache.setSource(terrainTypeYScale)

        val highlandMountainSelect = ModuleSelect()
        highlandMountainSelect.setLowSource(highlandTerrain)
        highlandMountainSelect.setHighSource(mountainTerrain)
        highlandMountainSelect.setControlSource(terrainTypeCache)
        highlandMountainSelect.setThreshold(0.55)
        highlandMountainSelect.setFalloff(0.2)

        val highlandLowlandSelect = ModuleSelect()
        highlandLowlandSelect.setLowSource(lowlandTerrain)
        highlandLowlandSelect.setHighSource(highlandMountainSelect)
        highlandLowlandSelect.setControlSource(terrainTypeCache)
        highlandLowlandSelect.setThreshold(0.25)
        highlandLowlandSelect.setFalloff(0.15)

        val highlandLowlandSelectCache = ModuleCache()
        highlandLowlandSelectCache.setSource(highlandLowlandSelect)

        val groundSelect = ModuleSelect()
        groundSelect.setLowSource(0.0)
        groundSelect.setHighSource(1.0)
        groundSelect.setThreshold(0.5)
        groundSelect.setControlSource(highlandLowlandSelectCache)

        val groundSelect2 = ModuleSelect()
        groundSelect2.setLowSource(0.0)
        groundSelect2.setHighSource(1.0)
        groundSelect2.setThreshold(0.8)
        groundSelect2.setControlSource(highlandLowlandSelectCache)

        /* caves */

        val caveShape = ModuleFractal()
        caveShape.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveShape.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveShape.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveShape.setNumOctaves(1)
        caveShape.setFrequency(params.caveShapeFreq) // TODO adjust the "density" of the caves
        caveShape.seed = seed shake caveMagic

        val caveAttenuateBias = ModuleBias()
        caveAttenuateBias.setSource(highlandLowlandSelectCache)
        caveAttenuateBias.setBias(params.caveAttenuateBias) // TODO (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids

        val caveShapeAttenuate = ModuleCombiner()
        caveShapeAttenuate.setType(ModuleCombiner.CombinerType.MULT)
        caveShapeAttenuate.setSource(0, caveShape)
        caveShapeAttenuate.setSource(1, caveAttenuateBias)

        val cavePerturbFractal = ModuleFractal()
        cavePerturbFractal.setType(ModuleFractal.FractalType.FBM)
        cavePerturbFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        cavePerturbFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        cavePerturbFractal.setNumOctaves(6)
        cavePerturbFractal.setFrequency(3.0)
        cavePerturbFractal.seed = seed shake cavePerturbMagic

        val cavePerturbScale = ModuleScaleOffset()
        cavePerturbScale.setSource(cavePerturbFractal)
        cavePerturbScale.setScale(0.45)
        cavePerturbScale.setOffset(0.0)

        val cavePerturb = ModuleTranslateDomain()
        cavePerturb.setSource(caveShapeAttenuate)
        cavePerturb.setAxisXSource(cavePerturbScale)

        val caveSelect = ModuleSelect()
        caveSelect.setLowSource(1.0)
        caveSelect.setHighSource(0.0)
        caveSelect.setControlSource(cavePerturb)
        caveSelect.setThreshold(params.caveSelectThre) // TODO also adjust this if you've touched the bias value. Number can be greater than 1.0
        caveSelect.setFalloff(0.0)

        val caveBlockageFractal = ModuleFractal()
        caveBlockageFractal.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveBlockageFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveBlockageFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveBlockageFractal.setNumOctaves(2)
        caveBlockageFractal.setFrequency(params.caveBlockageFractalFreq) // TODO same as caveShape frequency?
        caveBlockageFractal.seed = seed shake caveBlockageMagic

        // will only close-up deeper caves. Shallow caves will be less likely to be closed up
        val caveBlockageAttenuate = ModuleCombiner()
        caveBlockageAttenuate.setType(ModuleCombiner.CombinerType.MULT)
        caveBlockageAttenuate.setSource(0, caveBlockageFractal)
        caveBlockageAttenuate.setSource(1, caveAttenuateBias)

        val caveBlockageSelect = ModuleSelect()
        caveBlockageSelect.setLowSource(0.0)
        caveBlockageSelect.setHighSource(1.0)
        caveBlockageSelect.setControlSource(caveBlockageAttenuate)
        caveBlockageSelect.setThreshold(params.caveBlockageSelectThre) // TODO adjust cave cloing-up strength. Larger = more closing
        caveBlockageSelect.setFalloff(0.0)

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner()
        caveInMix.setType(ModuleCombiner.CombinerType.ADD)
        caveInMix.setSource(0, caveSelect)
        caveInMix.setSource(1, caveBlockageSelect)

        // this noise tree WILL generate noise value greater than 1.0
        // they should be treated properly when you actually generate the world out of the noisemap
        // for the visualisation, no treatment will be done in this demo app.

        val groundClamp = ModuleClamp()
        groundClamp.setRange(0.0, 100.0)
        groundClamp.setSource(highlandLowlandSelectCache)

        val groundScaling = ModuleScaleDomain()
        groundScaling.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
        groundScaling.setScaleY(1.0 / params.featureSize)
        groundScaling.setScaleZ(1.0 / params.featureSize)
        groundScaling.setSource(groundClamp)


        val caveClamp = ModuleClamp()
        caveClamp.setRange(0.0, 1.0)
        caveClamp.setSource(caveInMix)

        val caveScaling = ModuleScaleDomain()
        caveScaling.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
        caveScaling.setScaleY(1.0 / params.featureSize)
        caveScaling.setScaleZ(1.0 / params.featureSize)
        caveScaling.setSource(caveClamp)

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