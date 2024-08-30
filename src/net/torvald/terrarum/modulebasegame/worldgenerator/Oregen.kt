package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Created by minjaesong on 2023-10-25.
 */
class Oregen(world: GameWorld, isFinal: Boolean, private val caveAttenuateBiasScaledCache: ModuleCache, seed: Long, private val ores: List<OregenParams>) : Gen(world, isFinal, seed) {
    override fun getDone(loadscreen: LoadScreenBase?) {
        loadscreen?.let {
            it.stageValue += 1
            it.progress.set(0L)
        }

        Worldgen.threadExecutor.renew()
        submitJob(loadscreen)
        Worldgen.threadExecutor.join()
    }

    /**
     * @return List of noise instances, each instance refers to one of the spawnable ores
     */
    override fun getGenerator(seed: Long, any: Any?): List<Joise> {
        return ores.map {
            generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake it.tile, it.freq, it.power, it.scale, it.ratio)
        }
    }

    /**
     * Indices of `noises` has one-to-one mapping to the `ores`
     */
    override fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double) {
        for (x in xStart until xStart + CHUNK_W) {
            val st = (x.toDouble() / world.width) * TWO_PI

            for (y in yStart until yStart + CHUNK_H) {
                val sx = sin(st) * soff + soff // plus sampleOffset to make only
                val sz = cos(st) * soff + soff // positive points are to be sampled
                val sy = Worldgen.getSY(y)
                // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant

                // get the actual noise values
                // the size of the two lists are guaranteed to be identical as they all derive from the same `ores`
                val noiseValues = noises.map { it.get(sx, sy, sz) }
                val oreTiles = ores.map { it.tile }

                val tileToPut =
                    noiseValues.zip(oreTiles).firstNotNullOfOrNull { (n, tile) -> if (n > 0.5) tile else null }
                val backingTile = world.getTileFromTerrain(x, y)

                val blockTagNonGrata = ores.firstOrNull { it.tile == tileToPut }?.blockTagNonGrata ?: hashSetOf()
                val backingTileProp = BlockCodex[backingTile]

                if (tileToPut != null && backingTileProp.hasAllTagsOf("ROCK", "OREBEARING") && backingTileProp.hasNoTags(blockTagNonGrata)) {
                    // actually put the ore block
                    world.setTileOre(x, y, tileToPut, 0) // autotiling will be handled by the other worldgen process
                }
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

    private fun generateOreVeinModule(caveAttenuateBiasScaled: ModuleCache, seed: Long, freq: Double, pow: Double, scale: Double, ratio: Double): Joise {
        val oreShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
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
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
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

        val oreStrecth = ModuleScaleDomain().also {
            val xratio = if (ratio >= 1.0) ratio else 1.0
            val yratio = if (ratio < 1.0) 1.0 / ratio else 1.0
            val k = sqrt(2.0 / (xratio.sqr() + yratio.sqr()))
            val xs = xratio * k
            val ys = yratio * k

            it.setSource(orePerturb)
            it.setScaleX(1.0 / xs)
            it.setScaleZ(1.0 / xs)
            it.setScaleY(1.0 / ys)
        }

        val oreSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(oreStrecth)
            it.setThreshold(0.5)
            it.setFalloff(0.0)
        }

        return Joise(oreSelect)
    }
}

data class OregenParams(
    val tile: String, // ores@<modname>:<id>
    val freq: Double, //adjust the "density" of the caves
    val power: Double, // adjust the "concentration" of the cave gen. Lower = larger voids
    val scale: Double, // also adjust this if you've touched the bias value. Number can be greater than 1.0
    val ratio: Double, // how stretched the ore veins are. >1.0 = stretched horizontally, <1.0 = stretched vertically
    val tiling: String, // a16, a47, r16, r8
    val blockTagNonGrata: HashSet<String>,
)