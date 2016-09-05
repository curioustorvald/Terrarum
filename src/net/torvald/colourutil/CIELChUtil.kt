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

object CIELChUtil {

    /** Sweet LCh linear gradient */
    fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
        val from = fromCol.toLCh()
        val to = toCol.toLCh()
        val newL = FastMath.interpolateLinear(scale, from.L, to.L)
        val newC = FastMath.interpolateLinear(scale, from.C, to.C)
        val newAlpha = FastMath.interpolateLinear(scale, from.alpha, to.alpha)
        val newH: Float

        if ((from.h - to.h).abs() == FastMath.PI) // exact opposite colour
            return CIELabUtil.getGradient(scale, fromCol, toCol)
        else if ((from.h - to.h).abs() > FastMath.PI) // reflex angle
            newH = FastMath.interpolateLinear(scale, from.h, to.h + FastMath.TWO_PI)
        else
            newH = FastMath.interpolateLinear(scale, from.h, to.h)

        return CIELCh(newL, newC, newH, newAlpha).toRGB()
    }

    fun CIELab.toLCh(): CIELCh {
        val c = (a.sqr() + b.sqr()).sqrt()
        val h = FastMath.atan2(b, a)

        return CIELCh(L, c, h, alpha)
    }

    fun CIELCh.toLab(): CIELab {
        val a = C * FastMath.cos(h)
        val b = C * FastMath.sin(h)

        return CIELab(L, a, b, alpha)
    }

    private fun Color.toLCh() = this.toXYZ().toLab().toLCh()
    private fun CIELCh.toRGB() = this.toLab().toXYZ().toRGB()

    private fun Float.sqr() = this * this
    private fun Float.sqrt() = Math.sqrt(this.toDouble()).toFloat()

    private fun Float.abs() = FastMath.abs(this)
}

/**
 * @param L : Luminosity in 0.0 - 1.0
 * @param C : Chroma (saturation) in 0.0 - 1.0
 * @param h : Hue in radian (-pi to pi)
 */
data class CIELCh(var L: Float = 0f, var C: Float = 0f, var h: Float = 0f, var alpha: Float = 1f)
