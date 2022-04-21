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
import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.FloatVector.SPECIES_128

/**
 * A Cvec is kind of a Vector4f made compatible with LibGdx's Color class, with the intention of actually utilising
 * the JEP 338 VectorInstrinsics later, when the damned thing finally releases.
 *
 * Before then, the code will be identical to LibGdx's.
 *
 * THIS CLASS MUST NOT BE SERIALISED
 */

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
class Cvec {

    var vec = FloatVector.broadcast(SPECIES_128, 0f); private set

    /** the red, green, blue and alpha components  */
    val r: Float; get() = vec.lane(0)
    val g: Float; get() = vec.lane(1)
    val b: Float; get() = vec.lane(2)
    val a: Float; get() = vec.lane(3)

    /** Constructs a new Cvec with all components set to 0.  */
    constructor() {}

    /** @see .rgba8888ToCvec
     */
    constructor(rgba8888: Int) {
        rgba8888ToCvec(this, rgba8888)
    }

    constructor(scalar: Float) {
        vec = FloatVector.broadcast(SPECIES_128, scalar)
    }

    constructor(color: Color) {
        vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(color.r, color.g, color.b, color.a), 0)
    }

    constructor(vec: FloatVector) {
        this.vec = vec
    }

    /** Constructor, sets the components of the color
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     */
    constructor(r: Float, g: Float, b: Float, a: Float) {
        vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(r, g, b, a), 0)
    }

    /** Constructs a new color using the given color
     *
     * @param color the color
     */
    constructor(color: Cvec) {
        set(color)
    }

//    operator fun component1() = r
//    operator fun component2() = g
//    operator fun component3() = b
//    operator fun component4() = a

    /**
     * Get RGBA Element using index, of which:
     * - 0: R
     * - 1: G
     * - 2: B
     * - 3: A
     */
    inline fun getElem(index: Int) = vec.lane(index)

    /** Sets this color to the given color.
     *
     * @param color the Cvec
     */
    fun set(color: Cvec): Cvec {
        this.vec = color.vec
        return this
    }

    fun set(vec: FloatVector): Cvec {
        this.vec = vec
        return this
    }

    fun set(scalar: Float): Cvec {
        this.vec = FloatVector.broadcast(SPECIES_128, scalar)
        return this
    }

    /** Multiplies the this color and the given color
     *
     * @param color the color
     * @return this color.
     */
    fun mul(color: Cvec): Cvec {
        this.vec = this.vec.mul(color.vec)
        return this
    }

    /** Multiplies all components of this Cvec with the given value.
     *
     * @param value the value
     * @return this color
     */
    fun mul(value: Float): Cvec {
        this.vec = this.vec.mul(value)
        return this
    }

    /** Adds the given color to this color.
     *
     * @param color the color
     * @return this color
     */
    fun add(color: Cvec): Cvec {
        this.vec = this.vec.add(color.vec)
        return this
    }

    /** Subtracts the given color from this color
     *
     * @param color the color
     * @return this color
     */
    fun sub(color: Cvec): Cvec {
        this.vec = this.vec.sub(color.vec)
        return this
    }

    fun max(color: Cvec): Cvec {
        this.vec = this.vec.max(color.vec)
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
        vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(r, g, b, a), 0)
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
    fun toGdxColor() = Color(r, g, b, a)

    /** @return a copy of this color
     */
    fun cpy(): Cvec {
        return Cvec(this)
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
            val r = (value and -0x1000000).ushr(24) / 255f
            val g = (value and 0x00ff0000).ushr(16) / 255f
            val b = (value and 0x0000ff00).ushr(8) / 255f
            val a = (value and 0x000000ff) / 255f
            color.vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(r, g, b, a), 0)
        }

        /** Sets the Cvec components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
         * r, g, b) method
         *
         * @param color The Cvec to be modified.
         * @param value An integer color value in ARGB8888 format.
         */
        fun argb8888ToCvec(color: Cvec, value: Int) {
            val a = (value and -0x1000000).ushr(24) / 255f
            val r = (value and 0x00ff0000).ushr(16) / 255f
            val g = (value and 0x0000ff00).ushr(8) / 255f
            val b = (value and 0x000000ff) / 255f
            color.vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(r, g, b, a), 0)
        }

        /** Sets the Cvec components using the specified float value in the format ABGB8888.
         * @param color The Cvec to be modified.
         */
        fun abgr8888ToCvec(color: Cvec, value: Float) {
            val c = NumberUtils.floatToIntColor(value)
            val a = (c and -0x1000000).ushr(24) / 255f
            val b = (c and 0x00ff0000).ushr(16) / 255f
            val g = (c and 0x0000ff00).ushr(8) / 255f
            val r = (c and 0x000000ff) / 255f
            color.vec = FloatVector.fromArray(SPECIES_128, floatArrayOf(r, g, b, a), 0)
        }
    }
}
