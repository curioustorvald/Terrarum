package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameitems.isFluid
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2024-09-10.
 */
class Aquagen(world: GameWorld, isFinal: Boolean, val groundScalingCached: ModuleCache, seed: Long, params: Any) : Gen(world, isFinal, seed, params) {

    private val isAlpha2 = ((params as TerragenParams).versionSince >= 0x0000_000004_000004)

    private val FLUID_FILL = 1.2f

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
        if (INGAME.worldGenVer != null && !isAlpha2) return

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

                val lavaVal = noiseValues[noiseValues.lastIndex - 2]
                val lava = (lavaVal >= 0.5)

                val waterVal = noiseValues[noiseValues.lastIndex - 1]
                val waterShell = (waterVal >= 0.32)
                val water = (waterVal >= 0.5)

                val oilVal = noiseValues[noiseValues.lastIndex]
                val oilShell = (oilVal >= 0.38)
                val oil = (oilVal >= 0.5)

                val backingTile = world.getTileFromWall(x, y)

                val outFluid = if (water) Fluid.WATER
                else if (oil) Fluid.CRUDE_OIL
                else if (lava) Fluid.LAVA
                else if (waterShell) backingTile
                else if (oilShell) backingTile
                else null

                outFluid?.let {
                    if (outFluid.isFluid()) {
                        world.setTileTerrain(x, y, Block.AIR, true)
                        world.setFluid(x, y, outFluid, FLUID_FILL)
                    }
                    else {
                        world.setTileTerrain(x, y, outFluid, true)
                    }
                }
            }
        }
    }


    override fun getGenerator(seed: Long, params: Any?): List<Joise> {
        return listOf(
            Joise(generateSeaOfLava(seed)),
            Joise(generateAquifer(seed, groundScalingCached)),
            Joise(generateCrudeOil(seed, groundScalingCached)),
        )
    }

    private fun generateSeaOfLava(seed: Long): Module {
        val params = params as TerragenParams

        val lavaPipe = ModuleScaleDomain().also {
            it.setSource(ModuleFractal().also {
                it.setType(ModuleFractal.FractalType.RIDGEMULTI)
                it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
                it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
                it.setNumOctaves(1)
                it.setFrequency(params.lavaShapeFreg) // adjust the "density" of the caves
                it.seed = seed shake "LattiaOnLavaa"
            })
            it.setScaleY(1.0 / 6.0)
        }


        val lavaPerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(params.lavaShapeFreg * 3.0 / 4.0)
            it.seed = seed shake "FloorIsLava"
        }

        val lavaPerturbScale = ModuleScaleOffset().also {
            it.setSource(lavaPerturbFractal)
            it.setScale(23.0)
            it.setOffset(0.0)
        }

        val lavaPerturb = ModuleTranslateDomain().also {
            it.setSource(lavaPipe)
            it.setAxisXSource(lavaPerturbScale)
        }

        val lavaSelect = ModuleSelect().also {
            it.setLowSource(1.0)
            it.setHighSource(0.0)
            it.setControlSource(lavaPerturb)
            it.setThreshold(lavaGrad)
            it.setFalloff(0.0)
        }


        return lavaSelect
    }

    private fun generateAquifer(seed: Long, groundScalingCached: Module): Module {
        val params = params as TerragenParams

        val waterPocket = ModuleScaleDomain().also {
            it.setSource(ModuleFractal().also {
                it.setType(ModuleFractal.FractalType.BILLOW)
                it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
                it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
                it.setNumOctaves(4)
                it.setFrequency(params.rockBandCutoffFreq / params.featureSize)
                it.seed = seed shake "WaterPocket"
            })
            it.setScaleX(0.5)
            it.setScaleZ(0.5)
            it.setScaleY(0.8)
        }

        val terrainBool = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(groundScalingCached)
            it.setThreshold(0.5)
            it.setFalloff(0.1)
        }

        val aquifer = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, waterPocket)
            it.setSource(1, terrainBool)
            it.setSource(2, aquiferGrad)
        }


        return aquifer
    }

    private fun generateCrudeOil(seed: Long, groundScalingCached: Module): Module {
        val params = params as TerragenParams

        val oilPocket = ModuleScaleDomain().also {
            it.setSource(ModuleFractal().also {
                it.setType(ModuleFractal.FractalType.BILLOW)
                it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
                it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
                it.setNumOctaves(4)
                it.setFrequency(params.rockBandCutoffFreq / params.featureSize)
                it.seed = seed shake "CrudeOil"
            })
            it.setScaleX(0.16)
            it.setScaleZ(0.16)
            it.setScaleY(1.4)
        }

        crudeOilGradStart = TerrarumModuleCacheY().also {
            it.setSource(ModuleClamp().also {
                it.setSource(ModuleScaleOffset().also {
                    it.setSource(groundScalingCached)
                    it.setOffset(-8.0)
                })
                it.setRange(0.0, 1.0)
            })
        }

        crudeOilGrad = TerrarumModuleCacheY().also {
            it.setSource(ModuleCombiner().also {
                it.setType(ModuleCombiner.CombinerType.ADD)
                it.setSource(0, crudeOilGradStart)
                it.setSource(1, crudeOilGradEnd)
                it.setSource(2, ModuleConstant().also { it.setConstant(-1.0) })
            })
        }

        val oilLayer = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, oilPocket)
            it.setSource(1, crudeOilGrad)
        }


        return oilLayer
    }

    companion object {

        // val = sqrt((y-H+L) / L); where H=5300 (world height-100), L=620;
        // 100 is the height of the "base lava sheet", 600 is the height of the "transitional layer"
        // in this setup, the entire lava layer never exceeds 8 chunks (720 tiles) in height
        val lavaGrad = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleLavaFloorGrad().also {
                it.setH(5300.0)
                it.setL(620.0)
            })
        }

        val aquiferGrad = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleCaveLayerClosureGrad().also {
                it.setH(4300.0)
                it.setL(620.0)
            })
        }

        lateinit var crudeOilGradStart: TerrarumModuleCacheY
        lateinit var crudeOilGrad: TerrarumModuleCacheY

        val crudeOilGradEnd = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleCaveLayerClosureGrad().also {
                it.setH(4800.0)
                it.setL(620.0)
            })
        }

        val caveTerminalClosureGrad = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleCaveLayerClosureGrad().also {
                it.setH(17.2)
                it.setL(3.0)
            })
        }
        val aquiferTerminalClosureGrad = TerrarumModuleCacheY().also {
            it.setSource(TerrarumModuleCaveLayerClosureGrad().also {
                it.setH(21.0)
                it.setL(8.0)
            })
        }

    }
}