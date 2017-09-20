package net.torvald.colourutil

import com.badlogic.gdx.graphics.Color

/**
 * 6-Step RGB with builtin utils.
 * Created by minjaesong on 2016-02-11.
 */
class Col216 : LimitedColours {

    var raw: Byte = 0
        private set

    /**

     * @param data
     */
    constructor(data: Byte) {
        create(data.toInt())
    }

    /**

     * @param r 0-5
     * *
     * @param g 0-5
     * *
     * @param b 0-5
     */
    constructor(r: Int, g: Int, b: Int) {
        create(r, g, b)
    }

    override fun createGdxColor(raw: Int): Color {
        assertRaw(raw)
        val r = raw / MUL_2
        val g = raw % MUL_2 / MUL
        val b = raw % MUL

        return createGdxColor(r, g, b)
    }

    override fun createGdxColor(r: Int, g: Int, b: Int): Color {
        assertRGB(r, g, b)
        return Color((LOOKUP[r] shl 24) + (LOOKUP[g] shl 16) + (LOOKUP[b] shl 8) + 255)
    }

    override fun create(raw: Int) {
        assertRaw(raw)
        this.raw = raw.toByte()
    }

    override fun create(r: Int, g: Int, b: Int) {
        assertRGB(r, g, b)
        raw = (MUL_2 * r + MUL * g + b).toByte()
    }

    override fun toGdxColour(): Color = createGdxColor(raw.toUint())

    private fun assertRaw(i: Int) {
        if (i >= COLOUR_RANGE_SIZE || i < 0) {
            System.err.println("Illegal colour input: $i")
            throw IllegalArgumentException()
        }
    }

    private fun assertRGB(r: Int, g: Int, b: Int) {
        if (r !in 0..MAX_STEP || g !in 0..MAX_STEP || b !in 0..MAX_STEP) {
            System.err.println("Illegal colour input for channel r: $r")
            System.err.println("Illegal colour input for channel g: $g")
            System.err.println("Illegal colour input for channel b: $b")
            throw IllegalArgumentException()
        }
    }

    fun Byte.toUint() = this.toInt() and 0xFF

    companion object {
        @Transient private val LOOKUP = intArrayOf(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF)

        const val MUL = 6
        const val MUL_2 = MUL * MUL
        const val MAX_STEP = MUL - 1
        const val COLOUR_RANGE_SIZE = MUL_2 * MUL
    }
}
