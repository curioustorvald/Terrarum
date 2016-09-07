package net.torvald.colourutil

import com.jme3.math.FastMath
import org.newdawn.slick.Color
import net.torvald.colourutil.CIELabUtil.toXYZ
import net.torvald.colourutil.CIELabUtil.toRawRGB
import net.torvald.colourutil.CIELabUtil.toRGB

/**
 * A modification of CIEXYZ that is useful for additive mixtures of lights.
 *
 * reference: http://www.brucelindbloom.com/index.html?Equations.html
 *
 * If you're using Mac, you can play around with this colour space with
 * ColorSync Utility's calculator.
 *
 * Created by minjaesong on 16-09-06.
 */
object CIELuvUtil {

    fun Color.brighterLuv(scale: Float): Color {
        val brighten = scale + 1f

        val luv = this.toLuv()
        luv.L *= brighten
        return luv.toRGB()
    }

    fun Color.darkerLuv(scale: Float): Color {
        val darken = 1f - scale

        val luv = this.toLuv()
        luv.L *= darken
        return luv.toRGB()
    }

    /**
     * Alpha value will be overwritten to 1.0
     */
    infix fun Color.additiveLuv(other: Color): Color {
        val rgb = RGB(r, g, b) additiveLuv RGB(other.r, other.g, other.b)
        return Color(rgb.r, rgb.g, rgb.b, a)
    }

    /**
     * Alpha value will be overwritten to 1.0
     */
    infix fun RGB.additiveLuv(other: RGB): RGB {
        val thisLuv = this.toXYZ().toLuv()
        val otherLuv = other.toXYZ().toLuv()

        val newL = 1f - (1f - thisLuv.L) * (1 - otherLuv.L)
        val newU = thisLuv.u.times(otherLuv.L / newL) + otherLuv.u.times(otherLuv.L).times(1f - thisLuv.L).div(newL)
        val newV = thisLuv.v.times(otherLuv.L / newL) + otherLuv.v.times(otherLuv.L).times(1f - thisLuv.L).div(newL)

        return CIELuv(newL, newU, newV).toRawRGB()
    }

    fun CIEXYZ.toLuv(): CIELuv {
        val yRef = Y / D65.Y
        val uPrime = 4f * X / (X + 15f * Y + 3f * Z)
        val vPrime = 9f * Y / (X + 15f * Y + 3f * Z)
        val uRefPrime = 4f * D65.X / (D65.X + 15f * D65.Y + 3f * D65.Z)
        val vRefPrime = 9f * D65.Y / (D65.X + 15f * D65.Y + 3f * D65.Z)

        val L = if (yRef > epsilon)
            116f * yRef.cbrt() - 16f
        else
            kappa * yRef

        val u = 13f * L * (uPrime - uRefPrime)
        val v = 13f * L * (vPrime - vRefPrime)

        return CIELuv(L, u, v, alpha)
    }

    fun CIELuv.toXYZ(): CIEXYZ {
        val Y = if (L > kappa * epsilon)
            L.plus(16f).div(116f).cube()
        else
            L.div(kappa)
        val uRef = 4f * D65.X / (D65.X + 15f * D65.Y + 3f * D65.Z)
        val vRef = 9f * D65.Y / (D65.X + 15f * D65.Y + 3f * D65.Z)
        val a = (1f / 3f) * (52.times(L) / u.plus(13 * L * uRef)).minus(1f)
        val b = -5f * Y
        val c = -1f / 3f
        val d = Y * (39f.times(L) / v.plus(13f * L * vRef)).minus(5f)
        val X = (d - b) / (a - c)
        val Z = X * a + b

        return CIEXYZ(X, Y, Z, alpha)
    }

    fun Color.toLuv() = this.toXYZ().toLuv()
    fun CIELuv.toRawRGB() = this.toXYZ().toRawRGB()
    fun CIELuv.toRGB() = this.toXYZ().toRGB()

    private fun Float.cbrt() = FastMath.pow(this, 1f / 3f)
    private fun Float.cube() = this * this * this
}

data class CIELuv(var L: Float = 0f, var u: Float = 0f, var v: Float = 0f, val alpha: Float = 1f)
