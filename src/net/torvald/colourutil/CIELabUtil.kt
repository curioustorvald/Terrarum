package net.torvald.colourutil

import com.jme3.math.FastMath
import org.newdawn.slick.Color

/**
 * RGB in this code is always sRGB.
 * reference: http://www.brucelindbloom.com/index.html?Equations.html
 *
 * Created by minjaesong on 16-09-01.
 */
object CIELabUtil {
    fun Color.brighterLab(scale: Float): Color {
        val brighten = scale + 1f

        val lab = this.toLab()
        lab.L *= brighten
        return lab.toRGB()
    }

    fun Color.darkerLab(scale: Float): Color {
        val darken = 1f - scale

        val lab = this.toLab()
        lab.L *= darken
        return lab.toRGB()
    }

    /** Sweet Lab linear gradient */
    fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
        val from = fromCol.toLab()
        val to = toCol.toLab()
        val newL = FastMath.interpolateLinear(scale, from.L, to.L)
        val newA = FastMath.interpolateLinear(scale, from.a, to.a)
        val newB = FastMath.interpolateLinear(scale, from.b, to.b)
        val newAlpha = FastMath.interpolateLinear(scale, from.alpha, to.alpha)

        return CIELab(newL, newA, newB, newAlpha).toRGB()
    }

    private fun Color.toLab() = this.toXYZ().toLab()
    private fun CIELab.toRGB() = this.toXYZ().toRGB()

    fun Color.toXYZ(): CIEXYZ {
        val x = 0.4124564f * r + 0.3575761f * g + 0.1804375f * b
        val y = 0.2126729f * r + 0.7151522f * g + 0.0721750f * b
        val z = 0.0193339f * r + 0.1191920f * g + 0.9503041f * b

        return CIEXYZ(x, y, z, a)
    }

    fun CIEXYZ.toRGB(): Color {
        val r =  3.2404542f * x + -1.5371385f * y + -0.4985314f * z
        val g = -0.9692660f * x +  1.8760108f * y +  0.0415560f * z
        val b =  0.0556434f * x + -0.2040259f * y +  1.0572252f * z

        return Color(r, g, b, alpha)
    }

    fun CIEXYZ.toLab(): CIELab {
        val x = pivotXYZ(x / whitePoint.x)
        val y = pivotXYZ(y / whitePoint.y)
        val z = pivotXYZ(z / whitePoint.z)

        val L = Math.max(0f, 116 * y - 16)
        val a = 500 * (x - y)
        val b = 200 * (y - z)

        return CIELab(L, a, b, alpha)
    }

    fun CIELab.toXYZ(): CIEXYZ {
        val y = L.plus(16).div(116f)
        val x = a / 500f + y
        val z = y - b / 200f

        val x3 = x.cube()
        val z3 = z.cube()

        return CIEXYZ(
                whitePoint.x * if (x3 > epsilon) x3 else (x - 16f / 116f) / 7.787f,
                whitePoint.y * if (L > kappa * epsilon) (L.plus(16f) / 116f).cube() else L / kappa,
                whitePoint.z * if (z3 > epsilon) z3 else (z - 16f / 116f) / 7.787f,
                alpha
        )
    }

    private fun pivotXYZ(n: Float) = if (n > epsilon) n.cbrt() else (kappa * n + 16f) / 116f

    val epsilon = 0.008856f
    val kappa = 903.3f
    val whitePoint = CIEXYZ(95.047f, 100f, 108.883f)

    private fun Float.cbrt() = FastMath.pow(this, 1f / 3f)
    private fun Float.cube() = this * this * this
}

data class CIEXYZ(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, val alpha: Float = 1f)
data class CIELab(var L: Float = 0f, var a: Float = 0f, var b: Float = 0f, val alpha: Float = 1f)
