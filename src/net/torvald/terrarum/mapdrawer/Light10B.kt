package net.torvald.terrarum.mapdrawer

import com.jme3.math.FastMath
import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-07-08.
 */

class Light10B {
    
    private var data = 0

    constructor(r: Int, g: Int, b: Int) {
        data = constructRGBFromInt(r, g, b)
    }

    constructor(r: Float, g: Float, b: Float) {
        if (r < 0 || r > 1.0f) throw IllegalArgumentException("Red: out of range ($r)")
        if (g < 0 || g > 1.0f) throw IllegalArgumentException("Green: out of range ($g)")
        if (b < 0 || b > 1.0f) throw IllegalArgumentException("Blue: out of range ($b)")

        val intR = (r * CHANNEL_MAX).floor()
        val intG = (g * CHANNEL_MAX).floor()
        val intB = (b * CHANNEL_MAX).floor()

        data = constructRGBFromInt(intR, intG, intB)
    }

    constructor(raw: Int) {
        data = raw
    }

    constructor(color: Color) {
        data = constructRGBFromInt(color.red, color.green, color.blue)
    }

    /**
     * Assigners
     */
    fun fromRGB(r: Int, g: Int, b: Int): Light10B {
        data = constructRGBFromInt(r, g, b)
        return this
    }

    fun fromInt(raw: Int): Light10B {
        data = raw
        return this
    }

    fun fromSlickColor(color: Color): Light10B {
        data = constructRGBFromInt(color.red, color.green, color.blue)
        return this
    }



    fun toInt(): Int = data

    fun constructRGBFromInt(r: Int, g: Int, b: Int): Int {
        if (r !in 0..CHANNEL_MAX) throw IllegalArgumentException("Red: out of range ($r)")
        if (g !in 0..CHANNEL_MAX) throw IllegalArgumentException("Green: out of range ($g)")
        if (b !in 0..CHANNEL_MAX) throw IllegalArgumentException("Blue: out of range ($b)")
        return (r * STEPS_2 + g * STEPS + b)
    }

    val STEPS = 1024
    val STEPS_2 = STEPS * STEPS
    val CHANNEL_MAX = STEPS - 1
    val CHANNEL_MAX_FLOAT = CHANNEL_MAX.toFloat()
    val CHANNEL_MAX_DECIMAL = 4f
    val COLOUR_RANGE_SIZE = STEPS * STEPS_2

    override fun toString() = "10BitCol:$data"

    fun rawR() = data / STEPS_2
    fun rawG() = data % STEPS_2 / STEPS
    fun rawB() = data % STEPS

    fun r(): Float = this.rawR() / CHANNEL_MAX_FLOAT
    fun g(): Float = this.rawG() / CHANNEL_MAX_FLOAT
    fun b(): Float = this.rawB() / CHANNEL_MAX_FLOAT

    fun Float.floor() = FastMath.floor(this)
    private fun Float.clampChannel() = if (this < 0) 0f else if (this > CHANNEL_MAX_DECIMAL) CHANNEL_MAX_DECIMAL else this
    private fun Int.clampChannel() = if (this < 0) 0 else if (this > CHANNEL_MAX) CHANNEL_MAX else this

    operator fun plus(other: Light10B) =
            Light10B(
                    rawR().plus(other.rawR()).clampChannel(),
                    rawG().plus(other.rawG()).clampChannel(),
                    rawB().plus(other.rawB()).clampChannel()
            )
    operator fun minus(other: Light10B) =
            Light10B(
                    rawR().minus(other.rawR()).clampChannel(),
                    rawG().minus(other.rawG()).clampChannel(),
                    rawB().minus(other.rawB()).clampChannel()
            )
    operator fun times(other: Light10B) =
            Light10B(
                    r().times(other.r()).clampChannel(),
                    g().times(other.g()).clampChannel(),
                    b().times(other.b()).clampChannel()
            )
    infix fun darken(other: Light10B): Light10B {
        if (other.toInt() < 0 || other.toInt() >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("darken: out of range ($other)")

        val r = this.r() * (1f - other.r() * 6) // 6: Arbitrary value
        val g = this.g() * (1f - other.g() * 6) // TODO gamma correction?
        val b = this.b() * (1f - other.b() * 6)

        return Light10B(r.clampChannel(), g.clampChannel(), b.clampChannel())
    }
    infix fun darken(darken: Int): Light10B {
        if (darken < 0 || darken > CHANNEL_MAX)
            throw IllegalArgumentException("darken: out of range ($darken)")

        val darkenColoured = constructRGBFromInt(darken, darken, darken)
        return (this darken darkenColoured)
    }
    infix fun brighten(other: Light10B): Light10B {
        if (other.toInt() < 0 || other.toInt() >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("brighten: out of range ($other)")

        val r = this.r() * (1f + other.r() * 6) // 6: Arbitrary value
        val g = this.g() * (1f + other.g() * 6) // TODO gamma correction?
        val b = this.b() * (1f + other.b() * 6)

        return Light10B(r.clampChannel(), g.clampChannel(), b.clampChannel())
    }
    /**
     * Darken or brighten colour by 'brighten' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (-1.0 - 1.0) negative means darkening
     * @return processed colour
     */
    fun alterBrightnessUniform(data: Light10B, brighten: Float): Light10B {
        val modifier = if (brighten < 0)
            Light10B(-brighten, -brighten, -brighten)
        else
            Light10B(brighten, brighten, brighten)
        return if (brighten < 0)
            data darken modifier
        else
            data brighten modifier
    }

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * @param rgb2
     * @return
     */
    infix fun maxBlend(other: Light10B): Light10B {
        val r1 = this.rawR()
        val r2 = other.rawR()
        val newR = if (r1 > r2) r1 else r2
        val g1 = this.rawG()
        val g2 = other.rawG()
        val newG = if (g1 > g2) g1 else g2
        val b1 = this.rawB()
        val b2 = other.rawB()
        val newB = if (b1 > b2) b1 else b2

        return Light10B(newR, newG, newB)
    }

    infix fun screenBlend(other: Light10B): Light10B {
        val r1 = this.r()
        val r2 = other.r()
        val newR = 1 - (1 - r1) * (1 - r2)
        val g1 = this.g()
        val g2 = other.g()
        val newG = 1 - (1 - g1) * (1 - g2)
        val b1 = this.b()
        val b2 = other.b()
        val newB = 1 - (1 - b1) * (1 - b2)

        return Light10B(newR, newG, newB)
    }

    private infix fun colSub(other: Light10B) = Light10B(
            (this.rawR() - other.rawR()).clampChannel() ,
            (this.rawG() - other.rawG()).clampChannel() ,
            (this.rawB() - other.rawB()).clampChannel()
    )

    private infix fun colAdd(other: Light10B) = Light10B(
            (this.rawR() + other.rawR()).clampChannel() ,
            (this.rawG() + other.rawG()).clampChannel() ,
            (this.rawB() + other.rawB()).clampChannel()
    )
}