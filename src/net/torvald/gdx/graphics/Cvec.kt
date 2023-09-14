/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.torvald.gdx.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.NumberUtils

/**
 * A Cvec is kind of a Vector4f made compatible with LibGdx's Color class, with the intention of actually utilising
 * the JEP 338 VectorInstrinsics later, when the damned thing finally releases.
 *
 * Before then, the code will be identical to LibGdx's.
 */

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
class Cvec {

    /** the red, green, blue and alpha components  */
    @kotlin.jvm.JvmField var r: Float = 0f
    @kotlin.jvm.JvmField var g: Float = 0f
    @kotlin.jvm.JvmField var b: Float = 0f
    @kotlin.jvm.JvmField var a: Float = 0f

    var x: Float; get() = r; set(value) { r = value }
    var y: Float; get() = g; set(value) { g = value }
    var z: Float; get() = b; set(value) { b = value }
    var w: Float; get() = a; set(value) { a = value }

    /** Constructs a new Cvec with all components set to 0.  */
    constructor() {}

    /** @see .rgba8888ToCvec
     */
    constructor(rgba8888: Int) {
        rgba8888ToCvec(this, rgba8888)
    }

    constructor(color: Color) {
        this.r = color.r
        this.g = color.g
        this.b = color.b
        this.a = color.a
    }

    constructor(rgb: Color, alpha: Float) {
        this.r = rgb.r
        this.g = rgb.g
        this.b = rgb.b
        this.a = alpha
    }

    /** Constructor, sets the components of the color
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     */
    constructor(r: Float, g: Float, b: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    /** Constructs a new color using the given color
     *
     * @param color the color
     */
    constructor(color: Cvec) {
        set(color)
    }

    operator fun component1() = r
    operator fun component2() = g
    operator fun component3() = b
    operator fun component4() = a

    /**
     * Get RGBA Element using index, of which:
     * - 0: R
     * - 1: G
     * - 2: B
     * - 3: A
     */
    fun lane(index: Int) = when(index) {
        0 -> r
        1 -> g
        2 -> b
        3 -> a
        else -> throw IndexOutOfBoundsException("Invalid index $index")
    }

    /** Sets this color to the given color.
     *
     * @param color the Cvec
     */
    fun set(color: Cvec): Cvec {
        this.r = color.r
        this.g = color.g
        this.b = color.b
        this.a = color.a
        return this
    }

    /** Multiplies the this color and the given color
     *
     * @param color the color
     * @return this color.
     */
    fun mul(color: Cvec): Cvec {
        this.r *= color.r
        this.g *= color.g
        this.b *= color.b
        this.a *= color.a
        return this
    }

    /** Multiplies all components of this Cvec with the given value.
     *
     * @param value the value
     * @return this color
     */
    fun mul(value: Float): Cvec {
        this.r *= value
        this.g *= value
        this.b *= value
        this.a *= value
        return this
    }

    /** Adds the given color to this color.
     *
     * @param color the color
     * @return this color
     */
    fun add(color: Cvec): Cvec {
        this.r += color.r
        this.g += color.g
        this.b += color.b
        this.a += color.a
        return this
    }

    /** Subtracts the given color from this color
     *
     * @param color the color
     * @return this color
     */
    fun sub(color: Cvec): Cvec {
        this.r -= color.r
        this.g -= color.g
        this.b -= color.b
        this.a -= color.a
        return this
    }

    /** Sets this Cvec's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining
     */
    operator fun set(r: Float, g: Float, b: Float, a: Float): Cvec {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return this
    }

    /** Sets this color's component values through an integer representation.
     *
     * @return this Cvec for chaining
     * @see .rgba8888ToCvec
     */
    fun set(rgba: Int): Cvec {
        rgba8888ToCvec(this, rgba)
        return this
    }

    /** Adds the given color component values to this Cvec's values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining
     */
    fun add(r: Float, g: Float, b: Float, a: Float): Cvec {
        this.r += r
        this.g += g
        this.b += b
        this.a += a
        return this
    }

    /** Subtracts the given values from this Cvec's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining
     */
    fun sub(r: Float, g: Float, b: Float, a: Float): Cvec {
        this.r -= r
        this.g -= g
        this.b -= b
        this.a -= a
        return this
    }

    /** Multiplies this Cvec's color components by the given ones.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining
     */
    fun mul(r: Float, g: Float, b: Float, a: Float): Cvec {
        this.r *= r
        this.g *= g
        this.b *= b
        this.a *= a
        return this
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param target The target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    fun lerp(target: Cvec, t: Float): Cvec {
        this.r += t * (target.r - this.r)
        this.g += t * (target.g - this.g)
        this.b += t * (target.b - this.b)
        this.a += t * (target.a - this.a)
        return this
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param r The red component of the target color
     * @param g The green component of the target color
     * @param b The blue component of the target color
     * @param a The alpha component of the target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    fun lerp(r: Float, g: Float, b: Float, a: Float, t: Float): Cvec {
        this.r += t * (r - this.r)
        this.g += t * (g - this.g)
        this.b += t * (b - this.b)
        this.a += t * (a - this.a)
        return this
    }

    /** Multiplies the RGB values by the alpha.  */
    fun premultiplyAlpha(): Cvec {
        r *= a
        g *= a
        b *= a
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val color = other as Cvec?
        return toIntBits() == color!!.toIntBits()
    }

    override fun hashCode(): Int {
        var result = if (r != +0.0f) NumberUtils.floatToIntBits(r) else 0
        result = 31 * result + if (g != +0.0f) NumberUtils.floatToIntBits(g) else 0
        result = 31 * result + if (b != +0.0f) NumberUtils.floatToIntBits(b) else 0
        result = 31 * result + if (a != +0.0f) NumberUtils.floatToIntBits(a) else 0
        return result
    }

    /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see [NumberUtils.intToFloatColor]).
     * @return the packed color as a 32-bit float
     */
    fun toFloatBits(): Float {
        val color = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        return NumberUtils.intToFloatColor(color)
    }

    /** Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int.
     */
    fun toIntBits(): Int {
        return (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA.  */
    override fun toString(): String {
        var value = Integer
                .toHexString((255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt())
        while (value.length < 8)
            value = "0$value"
        return value
    }

    /** Sets the RGB Cvec components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
     * to preserve high range color and can range beyond typical values.
     * @param h The Hue in degree from 0 to 360
     * @param s The Saturation from 0 to 1
     * @param v The Value (brightness) from 0 to 1
     * @return The modified Cvec for chaining.
     */
    fun fromHsv(h: Float, s: Float, v: Float): Cvec {
        val x = (h / 60f + 6) % 6
        val i = x.toInt()
        val f = x - i
        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))
        when (i) {
            0    -> {
                r = v
                g = t
                b = p
            }
            1    -> {
                r = q
                g = v
                b = p
            }
            2    -> {
                r = p
                g = v
                b = t
            }
            3    -> {
                r = p
                g = q
                b = v
            }
            4    -> {
                r = t
                g = p
                b = v
            }
            else -> {
                r = v
                g = p
                b = q
            }
        }

        //return clamp();
        return this
    }

    /** Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
     * [.fromHsv]. This is the inverse of [.toHsv].
     * @param hsv The Hue, Saturation and Value components in that order.
     * @return The modified Cvec for chaining.
     */
    fun fromHsv(hsv: FloatArray): Cvec {
        return fromHsv(hsv[0], hsv[1], hsv[2])
    }

    fun toGdxColor() = Color(r, g, b, a)

    /** Extract Hue-Saturation-Value. This is the inverse of [.fromHsv].
     * @param hsv The HSV array to be modified.
     * @return HSV components for chaining.
     */
    fun toHsv(hsv: FloatArray): FloatArray {
        val max = Math.max(Math.max(r, g), b)
        val min = Math.min(Math.min(r, g), b)
        val range = max - min
        if (range == 0f) {
            hsv[0] = 0f
        }
        else if (max == r) {
            hsv[0] = (60 * (g - b) / range + 360) % 360
        }
        else if (max == g) {
            hsv[0] = 60 * (b - r) / range + 120
        }
        else {
            hsv[0] = 60 * (r - g) / range + 240
        }

        if (max > 0) {
            hsv[1] = 1 - min / max
        }
        else {
            hsv[1] = 0f
        }

        hsv[2] = max

        return hsv
    }

    /** @return a copy of this color
     */
    fun cpy(): Cvec {
        return Cvec(this)
    }

    /**
     * Creates a new Cvec that this Cvec transformed by the given function.
     *
     * @param transformation a function with two parameters: (`this.lane(i)`, `i`) and returns a float value
     */
    fun lanewise(transformation: (Float, Int) -> Float): Cvec {
        return Cvec(
                transformation(this.r, 0),
                transformation(this.g, 1),
                transformation(this.b, 2),
                transformation(this.a, 3)
        )
    }

    companion object {
        val WHITE = Cvec(1f, 1f, 1f, 1f)

        /** Returns a new color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        fun valueOf(hex: String): Cvec {
            var hex = hex
            hex = if (hex[0] == '#') hex.substring(1) else hex
            val r = Integer.valueOf(hex.substring(0, 2), 16)
            val g = Integer.valueOf(hex.substring(2, 4), 16)
            val b = Integer.valueOf(hex.substring(4, 6), 16)
            val a = if (hex.length != 8) 255 else Integer.valueOf(hex.substring(6, 8), 16)
            return Cvec(r / 255f, g / 255f, b / 255f, a / 255f)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
         * performance.
         * @param r the red component, 0 - 255
         * @param g the green component, 0 - 255
         * @param b the blue component, 0 - 255
         * @param a the alpha component, 0 - 255
         * @return the packed color as a 32-bit int
         */
        fun toIntBits(r: Int, g: Int, b: Int, a: Int): Int {
            return a shl 24 or (b shl 16) or (g shl 8) or r
        }

        fun alpha(alpha: Float): Int {
            return (alpha * 255.0f).toInt()
        }

        fun rgba8888(r: Float, g: Float, b: Float, a: Float): Int {
            return (r * 255).toInt() shl 24 or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()
        }

        fun argb8888(a: Float, r: Float, g: Float, b: Float): Int {
            return (a * 255).toInt() shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        fun rgba8888(color: Cvec): Int {
            return (color.r * 255).toInt() shl 24 or ((color.g * 255).toInt() shl 16) or ((color.b * 255).toInt() shl 8) or (color.a * 255).toInt()
        }

        fun argb8888(color: Cvec): Int {
            return (color.a * 255).toInt() shl 24 or ((color.r * 255).toInt() shl 16) or ((color.g * 255).toInt() shl 8) or (color.b * 255).toInt()
        }

        /** Sets the Cvec components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
         * b, a) method.
         *
         * @param color The Cvec to be modified.
         * @param value An integer color value in RGBA8888 format.
         */
        fun rgba8888ToCvec(color: Cvec, value: Int) {
            color.r = (value and -0x1000000).ushr(24) / 255f
            color.g = (value and 0x00ff0000).ushr(16) / 255f
            color.b = (value and 0x0000ff00).ushr(8) / 255f
            color.a = (value and 0x000000ff) / 255f
        }

        /** Sets the Cvec components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
         * r, g, b) method
         *
         * @param color The Cvec to be modified.
         * @param value An integer color value in ARGB8888 format.
         */
        fun argb8888ToCvec(color: Cvec, value: Int) {
            color.a = (value and -0x1000000).ushr(24) / 255f
            color.r = (value and 0x00ff0000).ushr(16) / 255f
            color.g = (value and 0x0000ff00).ushr(8) / 255f
            color.b = (value and 0x000000ff) / 255f
        }

        /** Sets the Cvec components using the specified float value in the format ABGB8888.
         * @param color The Cvec to be modified.
         */
        fun abgr8888ToCvec(color: Cvec, value: Float) {
            val c = NumberUtils.floatToIntColor(value)
            color.a = (c and -0x1000000).ushr(24) / 255f
            color.b = (c and 0x00ff0000).ushr(16) / 255f
            color.g = (c and 0x0000ff00).ushr(8) / 255f
            color.r = (c and 0x000000ff) / 255f
        }
    }
}
