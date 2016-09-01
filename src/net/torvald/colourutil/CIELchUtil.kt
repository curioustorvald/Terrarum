package net.torvald.colourutil

import com.jme3.math.FastMath
import net.torvald.colourutil.CIELabUtil.toLab
import net.torvald.colourutil.CIELabUtil.toRGB
import net.torvald.colourutil.CIELabUtil.toXYZ
import org.newdawn.slick.Color

/**
 * RGB in this code is always sRGB.
 * reference: http://www.brucelindbloom.com/index.html?Equations.html
 *
 * Created by minjaesong on 16-09-01.
 */

object CIELchUtil {

    /** Sweet Lch linear gradient */
    fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
        val from = fromCol.toLch()
        val to = toCol.toLch()
        val newL = FastMath.interpolateLinear(scale, from.L, to.L)
        val newC = FastMath.interpolateLinear(scale, from.c, to.c)
        val newAlpha = FastMath.interpolateLinear(scale, from.alpha, to.alpha)
        val newH: Float

        if ((from.h - to.h).abs() == FastMath.PI) // exact opposite colour
            return CIELabUtil.getGradient(scale, fromCol, toCol)
        else if ((from.h - to.h).abs() > FastMath.PI) // reflex angle
            newH = FastMath.interpolateLinear(scale, from.h, to.h + FastMath.TWO_PI)
        else
            newH = FastMath.interpolateLinear(scale, from.h, to.h)

        return CIELch(newL, newC, newH, newAlpha).toRGB()
    }

    fun CIELab.toLch(): CIELch {
        val c = (a.sqr() + b.sqr()).sqrt()
        val h = FastMath.atan2(b, a)

        return CIELch(L, c, h, alpha)
    }

    fun CIELch.toLab(): CIELab {
        val a = c * FastMath.cos(h)
        val b = c * FastMath.sin(h)

        return CIELab(L, a, b, alpha)
    }

    private fun Color.toLch() = this.toXYZ().toLab().toLch()
    private fun CIELch.toRGB() = this.toLab().toXYZ().toRGB()

    private fun Float.sqr() = this * this
    private fun Float.sqrt() = Math.sqrt(this.toDouble()).toFloat()

    private fun Float.abs() = FastMath.abs(this)
}

/**
 * @param L : Luminosity in 0.0 - 1.0
 * @param c : Chroma (saturation) in 0.0 - 1.0
 * @param h : Hue in radian (-pi to pi)
 */
data class CIELch(var L: Float = 0f, var c: Float = 0f, var h: Float = 0f, var alpha: Float = 1f)
