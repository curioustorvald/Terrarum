package net.torvald.terrarum.blockproperties

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.toRGB10
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.RGB10

/**
 * Created by minjaesong on 16-06-16.
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

    private fun getTorchFlicker(baseLum: Int): RGB10 {
        val funcY = FastMath.interpolateCatmullRom(0.0f, flickerFuncX.toFloat() / flickerFuncDomain,
                flickerP0, flickerP1, flickerP2, flickerP3
        )

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    private fun getSlowBreath(baseLum: Int): Int {
        val funcY = FastMath.sin(FastMath.PI * breathFuncX / breathCycleDuration) * breathRange

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    private fun getPulsate(baseLum: Int): Int {
        val funcY = FastMath.sin(FastMath.PI * pulsateFuncX / pulsateCycleDuration) * pulsateRange

        return LightmapRenderer.alterBrightnessUniform(baseLum, funcY)
    }

    /**
     * Using our own timer so that they flickers for same duration regardless of game's FPS
     */
    internal fun dynamicLumFuncTickClock() {
        // FPS-time compensation
        if (Gdx.graphics.framesPerSecond > 0) {
            flickerFuncX += Gdx.graphics.framesPerSecond
            breathFuncX += Gdx.graphics.framesPerSecond
            pulsateFuncX += Gdx.graphics.framesPerSecond
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

    fun getDynamicLumFunc(baseLum: Int, type: Int): Int {
        return when (type) {
            1    -> getTorchFlicker(baseLum)
            2    -> TerrarumGDX.ingame!!.world.globalLight // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(WorldTime.DAY_LENGTH / 2).toRGB10() // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }
}