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
    var flickerFuncX: Second = 0f // saves current status (time) of func
    val flickerFuncDomain: Second = 0.08f // time between two noise sample
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

    init {

    }

    private fun getTorchFlicker(baseLum: Cvec): Cvec {
        val funcY = FastMath.interpolateLinear(flickerFuncX / flickerFuncDomain, flickerP0, flickerP1)

        return alterBrightnessUniform(baseLum, funcY)
    }

    private fun getSlowBreath(baseLum: Cvec): Cvec {
        val funcY = FastMath.sin(FastMath.PI * breathFuncX / breathCycleDuration) * breathRange

        return alterBrightnessUniform(baseLum, funcY)
    }

    private fun getPulsate(baseLum: Cvec): Cvec {
        val funcY = FastMath.sin(FastMath.PI * pulsateFuncX / pulsateCycleDuration) * pulsateRange

        return alterBrightnessUniform(baseLum, funcY)
    }

    /**
     * Using our own timer so that they flickers for same duration regardless of game's FPS
     */
    internal fun dynamicLumFuncTickClock() {
        // FPS-time compensation
        if (Gdx.graphics.framesPerSecond > 0) {
            flickerFuncX += Gdx.graphics.rawDeltaTime
            breathFuncX  += Gdx.graphics.rawDeltaTime
            pulsateFuncX += Gdx.graphics.rawDeltaTime
        }

        // flicker-related vars
        if (flickerFuncX > flickerFuncDomain) {
            flickerFuncX -= flickerFuncDomain

            flickerP0 = flickerP1
            flickerP1 = getNewRandom()
        }

        // breath-related vars
        if (breathFuncX > breathCycleDuration) breathFuncX -= breathCycleDuration

        // pulsate-related vars
        if (pulsateFuncX > pulsateCycleDuration) pulsateFuncX -= pulsateCycleDuration
    }

    private fun getNewRandom() = random.nextFloat().times(2).minus(1f) * flickerFuncRange

    private fun linearInterpolation1D(a: Float, b: Float, x: Float) = a * (1 - x) + b * x

    fun getDynamicLumFunc(baseLum: Cvec, type: Int): Cvec {
        return when (type) {
            1    -> getTorchFlicker(baseLum)
            2    -> (Terrarum.ingame!!.world).globalLight.cpy().mul(LightmapRenderer.DIV_FLOAT) // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(Terrarum.ingame!!.world, WorldTime.DAY_LENGTH / 2).cpy().mul(LightmapRenderer.DIV_FLOAT) // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }

    /**
     * Darken or brighten colour by 'brighten' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (-1.0 - 1.0) negative means darkening
     * @return processed colour
     */
    private fun alterBrightnessUniform(data: Cvec, brighten: Float): Cvec {
        return Cvec(
                data.r + brighten,
                data.g + brighten,
                data.b + brighten,
                data.a + brighten
        )
    }
}