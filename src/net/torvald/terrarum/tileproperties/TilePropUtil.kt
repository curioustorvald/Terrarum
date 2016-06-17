package net.torvald.terrarum.tileproperties

import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.mapdrawer.LightmapRenderer

/**
 * Created by minjaesong on 16-06-16.
 */
object TilePropUtil {
    var flickerFuncX = 0 // in milliseconds; saves current status of func
    val flickerFuncDomain = 100 // time between two noise sample, in milliseconds
    val flickerFuncRange = 0.012f // intensity [0, 1]

    val random = HQRNG();
    var funcY = 0f

    var patternThis = getNewRandom()
    var patternNext = getNewRandom()

    init {

    }

    private fun getTorchFlicker(baseLum: Int): Int {
        funcY = linearInterpolation1D(patternThis, patternNext,
                flickerFuncX.toFloat() / flickerFuncDomain
        )

        return LightmapRenderer.brightenUniform(baseLum, funcY)
    }

    private fun getSlowBreath(baseLum: Int): Int {
        return baseLum
    }

    private fun getPulsate(baseLum: Int): Int {
        return baseLum
    }

    internal fun dynamicLumFuncTickClock() {
        if (Terrarum.appgc.fps > 0)
            flickerFuncX += 1000 / Terrarum.appgc.fps

        if (flickerFuncX > flickerFuncDomain) {
            flickerFuncX -= flickerFuncDomain

            patternThis = patternNext
            patternNext = getNewRandom()
        }
    }

    private fun getNewRandom() = random.nextFloat().times(2).minus(1f) * flickerFuncRange

    private fun linearInterpolation1D(a: Float, b: Float, x: Float) = a * (1 - x) + b * x;

    fun getDynamicLumFunc(baseLum: Int, type: Int): Int {
        return when (type) {
            1    -> getTorchFlicker(baseLum)
            2    -> Terrarum.game.map.globalLight // current global light
            3    -> Terrarum.game.globalLightByTime(WorldTime.DAY_LENGTH / 2) // daylight at noon
            4    -> getSlowBreath(baseLum)
            5    -> getPulsate(baseLum)
            else -> baseLum
        }
    }
}