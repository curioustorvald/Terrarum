package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_DIVISOR
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen.Companion.YHEIGHT_MAGIC
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.OrePlacement
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Created by minjaesong on 2023-10-25.
 */
class Oregen(world: GameWorld, private val caveAttenuateBiasScaled: ModuleScaleDomain, seed: Long, private val ores: List<OregenParams>) : Gen(world, seed) {

    private val threadExecutor = TerrarumIngame.worldgenThreadExecutor
    private val genSlices = max(threadExecutor.threadCount, world.width / 8)

    override fun getDone() {
        threadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).mapIndexed { i, xs ->
            threadExecutor.submit {
                val localJoise = getGenerator(seed)
                for (x in xs) {
                    val sampleTheta = (x.toDouble() / world.width) * TWO_PI
                    val sampleOffset = world.width / 8.0
                    draw(x, localJoise, sampleTheta, sampleOffset)
                }
            }
        }

        threadExecutor.join()
    }

    /**
     * @return List of noise instances, each instance refers to one of the spawnable ores
     */
    private fun getGenerator(seed: Long): List<Joise> {
        return ores.map {
            Joise(generateOreVeinModule(caveAttenuateBiasScaled, seed shake it.tile, it.freq, it.power, it.scale))
        }
    }

    /**
     * Indices of `noises` has one-to-one mapping to the `ores`
     */
    private fun draw(x: Int, noises: List<Joise>, st: Double, soff: Double) {
        for (y in 0 until world.height) {
            val sx = sin(st) * soff + soff // plus sampleOffset to make only
            val sz = cos(st) * soff + soff // positive points are to be sampled
            val sy = y - (world.height - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant
            // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant

            // get the actual noise values
            // the size of the two lists are guaranteed to be identical as they all derive from the same `ores`
            val noiseValues = noises.map { it.get(sx, sy, sz) }
            val oreTiles = ores.map { it.tile }

            val tileToPut = noiseValues.zip(oreTiles).firstNotNullOfOrNull { (n, tile) -> if (n > 0.5) tile else null }
            val backingTile = world.getTileFromTerrain(x, y)

            if (tileToPut != null && BlockCodex[backingTile].hasTag("ROCK")) {
                // actually put the ore block
                world.setTileOre(x, y, tileToPut, 0) // autotiling will be handled by the other worldgen process
            }
        }
    }


    private fun applyPowMult(joiseModule: Module, pow: Double, mult: Double): Module {
        return ModuleScaleOffset().also {
            it.setSource(ModulePow().also {
                it.setSource(joiseModule)
                it.setPower(pow)
            })
            it.setScale(mult)
        }
    }

    private fun generateOreVeinModule(caveAttenuateBiasScaled: ModuleScaleDomain, seed: Long, freq: Double, pow: Double, scale: Double): Module {
        val oreShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(freq) // adjust the "density" of the caves
            it.seed = seed
        }

        val oreShape2 = ModuleScaleOffset().also {
            it.setSource(oreShape)
            it.setScale(1.0)
            it.setOffset(-0.5)
        }

        val caveAttenuateBias3 = applyPowMult(caveAttenuateBiasScaled, pow, scale)

        val oreShapeAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, oreShape2)
            it.setSource(1, caveAttenuateBias3)
        }

        val orePerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(freq * 3.0 / 4.0)
            it.seed = seed shake 0x5721CE_76E_EA276L // strike the earth
        }

        val orePerturbScale = ModuleScaleOffset().also {
            it.setSource(orePerturbFractal)
            it.setScale(20.0)
            it.setOffset(0.0)
        }

        val orePerturb = ModuleTranslateDomain().also {
            it.setSource(oreShapeAttenuate)
            it.setAxisXSource(orePerturbScale)
        }

        val oreSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(orePerturb)
            it.setThreshold(0.5)
            it.setFalloff(0.0)
        }

        return oreSelect
    }
}

data class OregenParams(
    val tile: String,
    val freq: Double, //adjust the "density" of the caves
    val power: Double, // adjust the "concentration" of the cave gen. Lower = larger voids
    val scale: Double, // also adjust this if you've touched the bias value. Number can be greater than 1.0
)