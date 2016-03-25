package com.torvald.colourutil

import org.newdawn.slick.Color

/**
 * 6-Step RGB with builtin utils.
 * Created by minjaesong on 16-02-11.
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

    override fun createSlickColor(raw: Int): Color {
        assertRaw(raw)
        val r = raw / MUL_2
        val g = raw % MUL_2 / MUL
        val b = raw % MUL

        return createSlickColor(r, g, b)
    }

    override fun createSlickColor(r: Int, g: Int, b: Int): Color {
        assertRGB(r, g, b)
        return Color(LOOKUP[r], LOOKUP[g], LOOKUP[b])
    }

    override fun create(raw: Int) {
        assertRaw(raw)
        this.raw = raw.toByte()
    }

    override fun create(r: Int, g: Int, b: Int) {
        assertRGB(r, g, b)
        raw = (MUL_2 * r + MUL * g + b).toByte()
    }

    private fun assertRaw(i: Int) {
        if (i >= COLOUR_DOMAIN_SIZE || i < 0) {
            println("i: " + i.toString())
            throw IllegalArgumentException()
        }
    }

    private fun assertRGB(r: Int, g: Int, b: Int) {
        if (r !in 0..MAX_STEP || g !in 0..MAX_STEP || b !in 0..MAX_STEP) {
            println("r: " + r.toString())
            println("g: " + g.toString())
            println("b: " + b.toString())
            throw IllegalArgumentException()
        }
    }

    companion object {
        @Transient private val LOOKUP = intArrayOf(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF)

        const val MUL = 6
        const val MUL_2 = MUL * MUL
        const val MAX_STEP = MUL - 1
        const val COLOUR_DOMAIN_SIZE = MUL_2 * MUL
    }
}
