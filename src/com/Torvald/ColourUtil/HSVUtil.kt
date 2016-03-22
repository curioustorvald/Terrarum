package com.Torvald.ColourUtil

import com.jme3.math.FastMath
import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-01-16.
 */
object HSVUtil {

    /**
     * Convert HSV parameters to RGB color.
     * @param H 0-359 Hue
     * *
     * @param S 0-1 Saturation
     * *
     * @param V 0-1 Value
     * *
     * @return org.newdawn.slick.Color
     * *
     * @link http://www.rapidtables.com/convert/color/hsv-to-rgb.htm
     */
    fun toRGB(H: Float, S: Float, V: Float): Color {
        var H = H
        H %= 360f

        val C = V * S
        val X = C * (1 - FastMath.abs(
                H / 60f % 2 - 1))
        val m = V - C

        var R_prime = java.lang.Float.NaN
        var G_prime = java.lang.Float.NaN
        var B_prime = java.lang.Float.NaN

        if (H < 60) {
            R_prime = C
            G_prime = X
            B_prime = 0f
        }
        else if (H < 120) {
            R_prime = X
            G_prime = C
            B_prime = 0f
        }
        else if (H < 180) {
            R_prime = 0f
            G_prime = C
            B_prime = X
        }
        else if (H < 240) {
            R_prime = 0f
            G_prime = X
            B_prime = C
        }
        else if (H < 300) {
            R_prime = X
            G_prime = 0f
            B_prime = C
        }
        else if (H < 360) {
            R_prime = C
            G_prime = 0f
            B_prime = X
        }

        return Color(
                R_prime + m, G_prime + m, B_prime + m)
    }

    fun toRGB(hsv: HSV): Color {
        return toRGB(hsv.h, hsv.s, hsv.v)
    }

    fun fromRGB(color: Color): HSV {
        val r = color.r
        val g = color.g
        val b = color.b

        val rgbMin = FastMath.min(r, g, b)
        val rgbMax = FastMath.max(r, g, b)

        var h: Float
        val s: Float
        val v = rgbMax

        val delta = rgbMax - rgbMin

        if (rgbMax != 0f)
            s = delta / rgbMax
        else {
            h = 0f
            s = 0f
            return HSV(h, s, v)
        }

        if (r == rgbMax)
            h = (g - b) / delta
        else if (g == rgbMax)
            h = 2 + (b - r) / delta
        else
            h = 4 + (r - g) / delta

        h *= 60f
        if (h < 0) h += 360f

        return HSV(h, s, v)
    }

}
