package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2023-11-04.
 */
class Cavegen(world: GameWorld, isFinal: Boolean, val highlandLowlandSelectCache: ModuleCache, seed: Long, params: Any) : Gen(world, isFinal, seed, params) {

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


    //private fun draw(x: Int, y: Int, width: Int, height: Int, noiseValue: List<Double>, world: GameWorld) {
    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        for (x in xStart until xStart + CHUNK_W) {
            val st = (x.toDouble() / world.width) * TWO_PI

            for (y in yStart until yStart + CHUNK_H) {
                val sx = sin(st) * soff + soff // plus sampleOffset to make only
                val sz = cos(st) * soff + soff // positive points are to be sampled
                val sy = Worldgen.getSY(y)
                // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
                val noiseValue = noises.map { it.get(sx, sy, sz) }

                val cave = if (noiseValue[0] < 0.5) 0 else 1

                if (cave == 0) {
                    world.setTileTerrain(x, y, Block.AIR, true)
                }
            }
        }
    }


    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        val params = params as TerragenParams

        val caveMagic: Long = 0x00215741CDF // Urist McDF
        val cavePerturbMagic: Long = 0xA2410C // Armok
        val caveBlockageMagic: Long = 0xD15A57E5 // Disaster

        /* caves */

        val caveShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(1)
            it.setFrequency(params.caveShapeFreq) // adjust the "density" of the caves
            it.seed = seed shake caveMagic
        }

        val caveAttenuateBias0 = ModuleCache().also {it.setSource(ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        })}

        val caveAttenuateBias = ModuleScaleOffset().also {
            it.setSource(caveAttenuateBias0)
            it.setScale(params.caveAttenuateScale)
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

        val cavePerturb0 = ModuleTranslateDomain().also {
            it.setSource(caveShapeAttenuate)
            it.setAxisXSource(cavePerturbScale)
            if (isAlpha2)
                it.setAxisZSource(cavePerturbScale)
        }

        val cavePerturb = ModuleCombiner().also { // 0: rock, 1: air
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, cavePerturb0)
            // enable terminal closure for new worlds or ones generated from recent versions
            if (INGAME.worldGenVer == null || isAlpha2)
                it.setSource(1, caveTerminalClosureGrad)
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
            it.setThreshold(params.caveBlockageSelectThre) // adjust cave cloing-up strength. Lower = more closing
            it.setFalloff(0.0)
        }

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.ADD)
            it.setSource(0, caveSelect)
            it.setSource(1, caveBlockageSelect)
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


        return listOf(
            Joise(caveScaling)
        )
    }

    companion object {
        val caveTerminalClosureGrad = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleCaveLayerClosureGrad().also {
                it.setH(17.2)
                it.setL(3.0)
            })
        }
    }
}