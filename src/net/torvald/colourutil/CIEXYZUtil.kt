package net.torvald.colourutil

import com.badlogic.gdx.graphics.Color
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec

/**
 * Created by minjaesong on 2017-01-12.
 */

/**
 * 0..255 -> 0.0..1.0
 */
private val rgbLinLUT = FloatArray(256) {
    val step = it / 255f

    if (step > 0.04045f)
        ((step + 0.055f) / 1.055f).powerOf(2.4f)
    else step / 12.92f
}

/**
 * 0..255 -> 0.0..1.0
 */
private val rgbUnLinLUT = FloatArray(256) {
    val step = it / 255f

    if (step > 0.0031308f)
        1.055f * step.powerOf(1f / 2.4f) - 0.055f
    else
        step * 12.92f
}

private val rgbToXyzLut_XR = FloatArray(256) { 0.4124564f * (it / 255f) }


fun Color.brighterXYZ(scale: Float): Color {
    val xyz = this.toXYZ()
    xyz.X = xyz.X.times(1f + scale).clampOne()
    xyz.Y = xyz.Y.times(1f + scale).clampOne()
    xyz.Z = xyz.Z.times(1f + scale).clampOne()
    return xyz.toColor()
}

fun Color.darkerXYZ(scale: Float): Color {
    val xyz = this.toXYZ()
    xyz.X = xyz.X.times(1f - scale).clampOne()
    xyz.Y = xyz.Y.times(1f - scale).clampOne()
    xyz.Z = xyz.Z.times(1f - scale).clampOne()
    return xyz.toColor()
}

/*@Deprecated("Use one in CIELAB or CIELUV; CIEXYZ is not perceptually uniform")
fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
    val from = fromCol.toXYZ()
    val to = toCol.toXYZ()
    val newX = FastMath.interpolateLinear(scale, from.X, to.X)
    val newY = FastMath.interpolateLinear(scale, from.Y, to.Y)
    val newZ = FastMath.interpolateLinear(scale, from.Z, to.Z)
    val newAlpha = FastMath.interpolateLinear(scale, from.alpha, to.alpha)

    return CIEXYZ(newX, newY, newZ, newAlpha).toColor()
}*/

fun Color.toXYZ(): CIEXYZ = RGB(this).toXYZ()

/**
 * "Linearise" the sRGB triads. This use lookup table to speed up calculation.
 * Integer values (1/255, 2/255, .. , 254/255, 255/255) are accurate but any values in between are
 * linearly interpolated and thus slightly less accurate. Visually there's little-to-no difference,
 * but may not optimal for rigorous maths.
 */
/*fun RGB.linearise(): RGB {
    val out = floatArrayOf(0f, 0f, 0f)
    for (i in 0..2) {
        val value = when (i) {
            0 -> this.r
            1 -> this.g
            2 -> this.b
            else -> throw InternalError("Fuck you")
        }
        val step = value.clampOne() * 255f // 0.0 .. 255.0
        val intStep = step.toInt() // 0 .. 255
        val NeXTSTEP = minOf(intStep + 1, 255) // 1 .. 255

        out[i] = interpolateLinear(step - intStep, rgbLinLUT[intStep], rgbLinLUT[NeXTSTEP])
    }


    return RGB(out[0], out[1], out[2], alpha)
}*/

/** Suitable for rigorous maths but slower */
fun RGB.linearise(): RGB {
    val newR = if (r > 0.04045f)
        ((r + 0.055f) / 1.055f).powerOf(2.4f)
    else r / 12.92f
    val newG = if (g > 0.04045f)
        ((g + 0.055f) / 1.055f).powerOf(2.4f)
    else g / 12.92f
    val newB = if (b > 0.04045f)
        ((b + 0.055f) / 1.055f).powerOf(2.4f)
    else b / 12.92f


    return RGB(newR, newG, newB, alpha)
}

/**
 * "Un-linearise" the RGB triads. That is, codes the linear RGB into sRGB. This use lookup table to speed up calculation.
 * Integer values (1/255, 2/255, .. , 254/255, 255/255) are accurate but any values in between are
 * linearly interpolated and thus slightly less accurate. Visually there's little-to-no difference,
 * but may not optimal for rigorous maths.
 */
/*fun RGB.unLinearise(): RGB {
    val out = floatArrayOf(0f, 0f, 0f)
    for (i in 0..2) {
        val value = when (i) {
            0 -> this.r
            1 -> this.g
            2 -> this.b
            else -> throw InternalError("Fuck you")
        }
        val step = value.clampOne() * 255f // 0.0 .. 255.0
        val intStep = step.toInt() // 0 .. 255
        val NeXTSTEP = minOf(intStep + 1, 255) // 1 .. 255

        out[i] = interpolateLinear(step - intStep, rgbUnLinLUT[intStep], rgbUnLinLUT[NeXTSTEP])
    }


    return RGB(out[0], out[1], out[2], alpha)
}*/

/** Suitable for rigorous maths but slower */
fun RGB.unLinearise(): RGB {
    val newR = if (r > 0.0031308f)
        1.055f * r.powerOf(1f / 2.4f) - 0.055f
    else
        r * 12.92f
    val newG = if (g > 0.0031308f)
        1.055f * g.powerOf(1f / 2.4f) - 0.055f
    else
        g * 12.92f
    val newB = if (b > 0.0031308f)
        1.055f * b.powerOf(1f / 2.4f) - 0.055f
    else
        b * 12.92f


    return RGB(newR, newG, newB, alpha)
}

fun RGB.toXYZ(): CIEXYZ {
    val new = this.linearise()

    val x = 0.4124564f * new.r + 0.3575761f * new.g + 0.1804375f * new.b
    val y = 0.2126729f * new.r + 0.7151522f * new.g + 0.0721750f * new.b
    val z = 0.0193339f * new.r + 0.1191920f * new.g + 0.9503041f * new.b

    return CIEXYZ(x, y, z, alpha)
}

fun RGB.toColor() = Color(r, g, b, alpha)
fun RGB.toCvec() = Cvec(r, g, b, alpha)

fun CIEXYZ.toRGB(): RGB {
    val r =  3.2404542f * X - 1.5371385f * Y - 0.4985314f * Z
    val g = -0.9692660f * X + 1.8760108f * Y + 0.0415560f * Z
    val b =  0.0556434f * X - 0.2040259f * Y + 1.0572252f * Z

    return RGB(r, g, b, alpha).unLinearise()
}

fun CIEXYZ.toRGBRaw(): RGB {
    val r =  3.2404542f * X - 1.5371385f * Y - 0.4985314f * Z
    val g = -0.9692660f * X + 1.8760108f * Y + 0.0415560f * Z
    val b =  0.0556434f * X - 0.2040259f * Y + 1.0572252f * Z

    return RGB(r, g, b, alpha)
}

fun CIEXYZ.toColor(): Color {
    val rgb = this.toRGB()
    return Color(rgb.r, rgb.g, rgb.b, rgb.alpha)
}

fun CIEXYZ.mul(scalar: Float) = CIEXYZ(this.X * scalar, this.Y * scalar, this.Z * scalar, this.alpha)

fun CIEXYZ.toColorRaw(): Color {
    val rgb = this.toRGBRaw()
    return Color(rgb.r, rgb.g, rgb.b, rgb.alpha)
}

fun CIEYXY.toXYZ(): CIEXYZ {
    return CIEXYZ(x * yy / y, yy, (1f - x - y) * yy / y)
}

fun colourTempToXYZ(temp: Float, Y: Float): CIEXYZ {
    val x = if (temp < 7000)
        -4607000000f / FastMath.pow(temp, 3f) + 2967800f / FastMath.pow(temp, 2f) + 99.11f / temp + 0.244063f
    else
        -2006400000f / FastMath.pow(temp, 3f) + 1901800f / FastMath.pow(temp, 2f) + 247.48f / temp + 0.237040f

    val y = -3f * FastMath.pow(x, 2f) + 2.870f * x - 0.275f

    return CIEXYZ(x * Y / y, Y, (1 - x - y) * Y / y)
}

private fun Float.powerOf(exp: Float) = FastMath.pow(this, exp)
private fun Float.clampOne() = if (this > 1f) 1f else if (this < 0f) 0f else this

private fun interpolateLinear(scale: Float, startValue: Float, endValue: Float): Float {
    if (startValue == endValue) {
        return startValue
    }
    if (scale <= 0f) {
        return startValue
    }
    return if (scale >= 1f) {
        endValue
    }
    else (1f - scale) * startValue + scale * endValue
}


/** Range: X, Y, Z: 0 - 1.0+ (One-based-plus) */
data class CIEXYZ(var X: Float = 0f, var Y: Float = 0f, var Z: Float = 0f, var alpha: Float = 1f) {
    init {
        if (X.isNaN() && Y.isNaN() && Z.isNaN()) {
            this.X = 0f; this.Y = 0f; this.Z = 0f
        }
        //if (X !in -5f..5f || Y!in -5f..5f || Z !in -5f..5f)
        //    throw IllegalArgumentException("Value range error - this version of CIEXYZ is one-based (0.0 - 1.0+): ($X, $Y, $Z)")
    }
}

data class CIEYXY(val yy: Float = 0f, var x: Float = 0f, var y: Float = 0f, var alpha: Float = 1f) {
    init {
        if (yy < 0f || x < 0f || y < 0f)
            throw IllegalArgumentException("Value range error - parametres of YXY cannot be negative: ($yy, $x, $y)")
    }
}

/** Range: r, g, b: 0 - 1.0 (One-based) */
data class RGB(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var alpha: Float = 1f) {
    constructor(color: Color) : this() {
        r = color.r; g = color.g; b = color.b; alpha = color.a
    }
}