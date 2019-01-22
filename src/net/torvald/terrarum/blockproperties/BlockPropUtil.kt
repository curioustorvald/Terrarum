package net.torvald.terrarum.blockproperties

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.jme3.math.FastMath
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
    var flickerFuncX: Second = 0f // saves current status (time) of func
    val flickerFuncDomain: Second = 0.1f // time between two noise sample
    val flickerFuncRange = 0.012f // intensity [0, 1]

    var breathFuncX = 0f
    val breathRange = 0.02f
    val breathCycleDuration: Second = 2f

    var pulsateFuncX = 0f
    val pulsateRange = 0.034f
    val pulsateCycleDuration: Second = 0.5f

    val random = HQRNG()

    var flickerP0 = getNewRandom()
    var flickerP1 = getNewRandom()
    var flickerP2 = getNewRandom()
    var flickerP3 = getNewRandom()

    init {

    }

    private fun getTorchFlicker(baseLum: Color): Color {
        val funcY = FastMath.interpolateCatmullRom(0.0f, flickerFuncX / flickerFuncDomain,
                flickerP0, flickerP1, flickerP2, flickerP3
        )

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    private fun getSlowBreath(baseLum: Color): Color {
        val funcY = FastMath.sin(FastMath.PI * breathFuncX / breathCycleDuration) * breathRange

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    private fun getPulsate(baseLum: Color): Color {
        val funcY = FastMath.sin(FastMath.PI * pulsateFuncX / pulsateCycleDuration) * pulsateRange

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    /**
     * Using our own timer so that they flickers for same duration regardless of game's FPS
     */
    internal fun dynamicLumFuncTickClock() {
        // FPS-time compensation
        if (Gdx.graphics.framesPerSecond > 0) {
            flickerFuncX += Gdx.graphics.deltaTime * 1000f
            breathFuncX  += Gdx.graphics.deltaTime * 1000f
            pulsateFuncX += Gdx.graphics.deltaTime * 1000f
        }

        // flicker-related vars
        if (flickerFuncX > flickerFuncDomain) {
            flickerFuncX -= flickerFuncDomain

            //flickerPatternThis = flickerPatternNext
            //flickerPatternNext = getNewRandom()
            flickerP0 = flickerP1
            flickerP1 = flickerP2
            flickerP2 = flickerP3
            flickerP3 = getNewRandom()
        }

        // breath-related vars
        if (breathFuncX > breathCycleDuration) breathFuncX -= breathCycleDuration

        // pulsate-related vars
        if (pulsateFuncX > pulsateCycleDuration) pulsateFuncX -= pulsateCycleDuration
    }

    private fun getNewRandom() = random.nextFloat().times(2).minus(1f) * flickerFuncRange

    private fun linearInterpolation1D(a: Float, b: Float, x: Float) = a * (1 - x) + b * x

    fun getDynamicLumFunc(baseLum: Color, type: Int): Color {
        return when (type) {
            1    -> getTorchFlicker(baseLum)
            2    -> (Terrarum.ingame!!.world).globalLight.cpy().mul(LightmapRenderer.DIV_FLOAT) // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(WorldTime.DAY_LENGTH / 2).cpy().mul(LightmapRenderer.DIV_FLOAT) // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }
}