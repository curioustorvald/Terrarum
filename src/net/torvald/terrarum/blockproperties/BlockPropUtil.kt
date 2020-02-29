package net.torvald.terrarum.blockproperties

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.LightmapRenderer

/**
 * Created by minjaesong on 2016-06-16.
 */
object BlockPropUtil {
    //var flickerFuncX: Second = 0f // saves current status (time) of func
    val flickerFuncDomain: Second = 0.08f // time between two noise sample
    val flickerFuncRange = 0.012f // intensity [0, 1]

    //var breathFuncX = 0f
    val breathRange = 0.02f
    val breathCycleDuration: Second = 2f

    //var pulsateFuncX = 0f
    val pulsateRange = 0.034f
    val pulsateCycleDuration: Second = 0.5f

    val random = HQRNG()

    //var flickerP0 = getNewRandom()
    //var flickerP1 = getNewRandom()

    init {

    }

    private fun getTorchFlicker(prop: BlockProp): Cvec {
        val funcY = FastMath.interpolateLinear(prop.rngBase0 / flickerFuncDomain, prop.rngBase1, prop.rngBase2)
        return alterBrightnessUniform(prop.baseLumCol, funcY)
    }

    private fun getSlowBreath(prop: BlockProp): Cvec {
        val funcY = FastMath.sin(FastMath.PI * prop.rngBase0 / breathCycleDuration) * breathRange
        return alterBrightnessUniform(prop.baseLumCol, funcY)
    }

    private fun getPulsate(prop: BlockProp): Cvec {
        val funcY = FastMath.sin(FastMath.PI * prop.rngBase0 / pulsateCycleDuration) * pulsateRange
        return alterBrightnessUniform(prop.baseLumCol, funcY)
    }


    /**
     * Using our own timer so that they flickers for same duration regardless of game's FPS
     */
    internal fun dynamicLumFuncTickClock() {

        // update the memoised values in props
        /*for (key in BlockCodex.dynamicLights) {
            try {
                val prop = BlockCodex[key]
                if (prop.dynamicLuminosityFunction != 0) {
                    prop.lumCol.set(getDynamicLumFunc(prop.baseLumCol, prop.dynamicLuminosityFunction))
                    prop.lumColR = prop.lumCol.r
                    prop.lumColG = prop.lumCol.g
                    prop.lumColB = prop.lumCol.b
                    prop.lumColA = prop.lumCol.a
                }
            }
            catch (skip: NullPointerException) {}
        }*/
        // update randomised virtual props instead
        for (keyMax in BlockCodex.dynamicToVirtualPropMapping) {
            repeat(BlockCodex.DYNAMIC_RANDOM_CASES) {
                val prop = BlockCodex[keyMax.second - it]
                val domain = when (prop.dynamicLuminosityFunction) {
                    1 -> flickerFuncDomain
                    4 -> breathCycleDuration
                    5 -> pulsateCycleDuration
                    else -> 0f
                }

                // FPS-time compensation
                if (Gdx.graphics.framesPerSecond > 0) {
                    prop.rngBase0 += Gdx.graphics.rawDeltaTime
                }

                // reset timer
                if (prop.rngBase0 > domain) {
                    prop.rngBase0 -= domain

                    // flicker related
                    prop.rngBase1 = prop.rngBase2
                    prop.rngBase2 = getNewRandom()
                }

                prop._lumCol = getDynamicLumFunc(prop)
                //prop.lumColR = prop.lumCol.r
                //prop.lumColG = prop.lumCol.g
                //prop.lumColB = prop.lumCol.b
                //prop.lumColA = prop.lumCol.a
            }
        }
    }

    private fun getNewRandom() = random.nextFloat().times(2).minus(1f) * flickerFuncRange

    private fun linearInterpolation1D(a: Float, b: Float, x: Float) = a * (1 - x) + b * x

    private fun getDynamicLumFunc(prop: BlockProp): Cvec {
        return when (prop.dynamicLuminosityFunction) {
            1    -> getTorchFlicker(prop)
            2    -> (Terrarum.ingame!!.world).globalLight * LightmapRenderer.DIV_FLOAT_VEC // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(Terrarum.ingame!!.world, WorldTime.DAY_LENGTH / 2) * LightmapRenderer.DIV_FLOAT_VEC // daylight at noon
            4    -> getSlowBreath(prop)
            5    -> getPulsate(prop)
            else -> prop.baseLumCol
        }
    }

    /**
     * @param chan 0 for R, 1 for G, 2 for B, 3 for A
     */
    /*private fun getDynamicLumFuncByChan(baseLum: Float, type: Int, chan: Int): Float {
        return when (type) {
            1    -> getTorchFlicker(baseLum)
            2    -> (Terrarum.ingame!!.world).globalLight.cpy().mul(LightmapRenderer.DIV_FLOAT).getElem(chan) // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(Terrarum.ingame!!.world, WorldTime.DAY_LENGTH / 2).cpy().mul(LightmapRenderer.DIV_FLOAT).getElem(chan) // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }*/

    /**
     * Darken or brighten colour by 'brighten' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (-1.0 - 1.0) negative means darkening
     * @return processed colour
     */
    private fun alterBrightnessUniform(data: Cvec, brighten: Float): Cvec {
        return data + Cvec(brighten)
    }
}