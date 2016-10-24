package net.torvald.colourutil

import com.jme3.math.FastMath
import net.torvald.colourutil.CIELabUtil.toLab
import net.torvald.colourutil.CIELabUtil.toRGB
import net.torvald.colourutil.CIELabUtil.toRawRGB
import net.torvald.colourutil.CIELabUtil.toXYZ
import org.newdawn.slick.Color

/**
 * A modification of CIEXYZ that is useful for surface colours
 *
 * If you are trying to mix some colour with other, especially one that
 * requires additive mixing (such as illuminant), USE CIELuvUtil, this is
 * not for you.
 *
 * RGB in this code is always sRGB.
 * reference: http://www.brucelindbloom.com/index.html?Equations.html
 *
 * If you're using Mac, you can play around with this colour space with
 * ColorSync Utility's calculator.
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

    fun RGB.toXYZ(): CIEXYZ {
        val newR = if (r > 0.04045f)
            ((r + 0.055f) / 1.055f).powerOf(2.4f)
        else r / 12.92f
        val newG = if (g > 0.04045f)
            ((g + 0.055f) / 1.055f).powerOf(2.4f)
        else g / 12.92f
        val newB = if (b > 0.04045f)
            ((b + 0.055f) / 1.055f).powerOf(2.4f)
        else b / 12.92f

        val x = 0.4124564f * newR + 0.3575761f * newG + 0.1804375f * newB
        val y = 0.2126729f * newR + 0.7151522f * newG + 0.0721750f * newB
        val z = 0.0193339f * newR + 0.1191920f * newG + 0.9503041f * newB

        return CIEXYZ(x, y, z, alpha)
    }

    fun Color.toXYZ(): CIEXYZ = RGB(this).toXYZ()

    fun CIEXYZ.toRawRGB(): RGB {
        var r = 3.2404542f * X - 1.5371385f * Y - 0.4985314f * Z
        var g = -0.9692660f * X + 1.8760108f * Y + 0.0415560f * Z
        var b = 0.0556434f * X - 0.2040259f * Y + 1.0572252f * Z

        if (r > 0.0031308f)
            r = 1.055f * r.powerOf(1f / 2.4f) - 0.055f
        else
            r *= 12.92f
        if (g > 0.0031308f)
            g = 1.055f * g.powerOf(1f / 2.4f) - 0.055f
        else
            g *= 12.92f
        if (b > 0.0031308f)
            b = 1.055f * b.powerOf(1f / 2.4f) - 0.055f
        else
            b *= 12.92f

        return RGB(r, g, b, alpha)
    }

    fun CIEXYZ.toRGB(): Color {
        val rgb = this.toRawRGB()
        return Color(rgb.r, rgb.g, rgb.b, rgb.alpha)
    }

    fun CIEXYZ.toLab(): CIELab {
        val x = pivotXYZ(X / D65.X)
        val y = pivotXYZ(Y / D65.Y)
        val z = pivotXYZ(Z / D65.Z)

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
                D65.X * if (x3 > epsilon) x3 else (x - 16f / 116f) / 7.787f,
                D65.Y * if (L > kappa * epsilon) (L.plus(16f) / 116f).cube() else L / kappa,
                D65.Z * if (z3 > epsilon) z3 else (z - 16f / 116f) / 7.787f,
                alpha
        )
    }

    private fun pivotXYZ(n: Float) = if (n > epsilon) n.cbrt() else (kappa * n + 16f) / 116f

    private fun Float.cbrt() = FastMath.pow(this, 1f / 3f)
    private fun Float.cube() = this * this * this
    private fun Float.powerOf(exp: Float) = FastMath.pow(this, exp)
}

fun Color.toLab() = this.toXYZ().toLab()
fun RGB.toLab() = this.toXYZ().toLab()
fun CIELab.toRGB() = this.toXYZ().toRGB()
fun CIELab.toRawRGB() = this.toXYZ().toRawRGB()

internal val D65 = CIEXYZ(0.95047f, 1.00f, 1.08883f)
val epsilon = 216f/24389f
val kappa = 24389f/27f

/** Range: X, Y, Z: 0 - 1.0+ (One-based-plus) */
data class CIEXYZ(var X: Float = 0f, var Y: Float = 0f, var Z: Float = 0f, val alpha: Float = 1f) {
    init {
        if (X > 2f || Y > 2f || Z > 2f)
            throw IllegalArgumentException("Value range error - CIEXYZ is one-based (0.0 - 1.0+): ($X, $Y, $Z)")
    }
}
/**
 * Range:
 * L: 0-100.0
 * u, v: -100+ - 100+
 * (Hundred-based-plus)
 */
data class CIELab(var L: Float = 0f, var a: Float = 0f, var b: Float = 0f, val alpha: Float = 1f)
/** Range: r, g, b: 0 - 1.0 (One-based) */
data class RGB(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, val alpha: Float = 1f) {
    constructor(color: Color) : this() {
        r = color.r; g = color.g; b = color.b
    }
}
