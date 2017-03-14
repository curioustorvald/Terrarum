package net.torvald.terrarum.tileproperties

import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.toInt
import net.torvald.terrarum.weather.WeatherMixer
import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-06-16.
 */
object TilePropUtil {
    var flickerFuncX: Millisec = 0 // in milliseconds; saves current status (time) of func
    val flickerFuncDomain: Millisec = 100 // time between two noise sample, in milliseconds
    val flickerFuncRange = 0.012f // intensity [0, 1]

    var breathFuncX = 0
    val breathRange = 0.02f
    val breathCycleDuration: Millisec = 2000 // in milliseconds

    var pulsateFuncX = 0
    val pulsateRange = 0.034f
    val pulsateCycleDuration: Millisec = 500 // in milliseconds

    val random = HQRNG()

    var flickerP0 = getNewRandom()
    var flickerP1 = getNewRandom()
    var flickerP2 = getNewRandom()
    var flickerP3 = getNewRandom()

    init {

    }

    private fun getTorchFlicker(baseLum: Int): Int {
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

    internal fun dynamicLumFuncTickClock() {
        // FPS-time compensation
        if (Terrarum.appgc.fps > 0) {
            flickerFuncX += 1000 / Terrarum.appgc.fps
            breathFuncX += 1000 / Terrarum.appgc.fps
            pulsateFuncX += 1000 / Terrarum.appgc.fps
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
            2    -> Terrarum.ingame!!.world.globalLight // current global light
            3    -> WeatherMixer.getGlobalLightOfTime(WorldTime.DAY_LENGTH / 2).toInt() // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }
}