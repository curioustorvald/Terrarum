package net.torvald.terrarum.tileproperties

import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.mapdrawer.LightmapRenderer

/**
 * Created by minjaesong on 16-06-16.
 */
object TilePropUtil {
    var flickerFuncX = 0 // in milliseconds; saves current status of func
    val flickerFuncDomain = 50 // time between two noise sample, in milliseconds
    val flickerFuncRange = 0.012f // intensity [0, 1]
    //val torchIntensityOffset = -0.04f

    val random = HQRNG();
    var funcY = 0f

    var patternThis = getNewPattern()
    var patternNext = getNewPattern()

    init {

    }

    fun getTorchFlicker(baseLum: Int): Int {
        funcY = linearInterpolation1D(patternThis, patternNext,
                flickerFuncX.toFloat() / flickerFuncDomain
        )

        return LightmapRenderer.brightenUniform(baseLum, funcY)
    }

    fun torchFlickerTickClock() {
        flickerFuncX += Terrarum.game.DELTA_T

        if (flickerFuncX > flickerFuncDomain) {
            flickerFuncX -= flickerFuncDomain

            patternThis = patternNext
            patternNext = getNewPattern()
        }
    }

    private fun getNewPattern(): Float = random.nextFloat().times(2).minus(1f) * flickerFuncRange

    private fun cosineInterpolation1D(a: Float, b: Float, x: Float): Float{
        val ft: Float = x * FastMath.PI;
        val f: Float = (1 - FastMath.cos(ft)) * 0.5f;

        return a * (1 - f) + b * f;
    }

    private fun linearInterpolation1D(a: Float, b: Float, x: Float) =
            a * (1 - x) + b * x;
}