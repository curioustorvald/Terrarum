package net.torvald.colourutil

import com.jme3.math.FastMath
import net.torvald.colourutil.CIELabUtil.toLab
import net.torvald.colourutil.CIEXYZUtil.toColor
import net.torvald.colourutil.CIEXYZUtil.toRGB
import net.torvald.colourutil.CIEXYZUtil.toXYZ
import net.torvald.colourutil.CIELabUtil.toXYZ
import com.badlogic.gdx.graphics.Color

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
 * Created by minjaesong on 2016-09-01.
 */
object CIELabUtil {
    fun Color.brighterLab(scale: Float): Color {
        val brighten = scale + 1f

        val lab = this.toLab()
        lab.L *= brighten
        return lab.toColor()
    }

    fun Color.darkerLab(scale: Float): Color {
        val darken = 1f - scale

        val lab = this.toLab()
        lab.L *= darken
        return lab.toColor()
    }

    /** Sweet Lab linear gradient */
    fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
        val from = fromCol.toLab()
        val to = toCol.toLab()
        val newL = FastMath.interpolateLinear(scale, from.L, to.L)
        val newA = FastMath.interpolateLinear(scale, from.a, to.a)
        val newB = FastMath.interpolateLinear(scale, from.b, to.b)
        val newAlpha = FastMath.interpolateLinear(scale, from.alpha, to.alpha)

        return CIELab(newL, newA, newB, newAlpha).toColor()
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
fun CIELab.toColor() = this.toXYZ().toColor()
fun CIELab.toRGB() = this.toXYZ().toRGB()

internal val D65 = CIEXYZ(0.95047f, 1f, 1.08883f)
val epsilon = 216f/24389f
val kappa = 24389f/27f

/**
 * Range:
 * L: 0-100.0
 * u, v: -100+ - 100+
 * (Hundred-based-plus)
 */
data class CIELab(var L: Float = 0f, var a: Float = 0f, var b: Float = 0f, var alpha: Float = 1f)

