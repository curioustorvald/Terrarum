package com.Torvald.ColourUtil

import org.newdawn.slick.Color

/**
 * 40-Step RGB with builtin utils.
 * Created by minjaesong on 16-02-20.
 */
class Col40 : LimitedColours {

    var raw: Char = ' '
        private set

    override fun createSlickColor(raw: Int): Color {
        assertRaw(raw)
        val r = raw / MUL_2
        val g = raw % MUL_2 / MUL
        val b = raw % MUL

        return createSlickColor(r, g, b)
    }

    override fun createSlickColor(r: Int, g: Int, b: Int): Color {
        assertRGB(r, g, b)
        return Color(LOOKUP[r] shl 16 or (LOOKUP[g] shl 8) or LOOKUP[b])
    }

    override fun create(raw: Int) {
        assertRaw(raw)
        this.raw = raw.toChar()
    }

    override fun create(r: Int, g: Int, b: Int) {
        assertRGB(r, g, b)
        raw = (MUL_2 * r + MUL * g + b).toChar()
    }

    constructor() {
    }

    constructor(c: Color) {
        create(
                Math.round(c.r * (MUL - 1)),
                Math.round(c.g * (MUL - 1)),
                Math.round(c.b * (MUL - 1)))
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
        @Transient private val LOOKUP = intArrayOf(0, 7, 13, 20, 26, 33, 39, 46, 52, 59, 65, 72, 78, 85, 92, 98, 105, 111, 118, 124, 131, 137, 144, 150, 157, 163, 170, 177, 183, 190, 196, 203, 209, 216, 222, 229, 235, 242, 248, 255)

        const val MUL = 40
        const val MUL_2 = MUL * MUL
        const val MAX_STEP = MUL - 1
        const val COLOUR_DOMAIN_SIZE = MUL_2 * MUL
    }
}
