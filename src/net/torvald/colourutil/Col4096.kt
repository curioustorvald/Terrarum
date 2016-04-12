package net.torvald.colourutil

import org.newdawn.slick.Color

/**
 * 12-bit (16-step) RGB with builtin utils.
 * Created by minjaesong on 16-01-23.
 */
class Col4096 : LimitedColours {

    /**
     * Retrieve raw ARGB value
     * @return 0xARGB
     */
    var raw: Short = 0
        private set

    /**

     * @param data
     */
    constructor(data: Int) {
        create(data)
    }

    /**

     * @param r 0-15
     * *
     * @param g 0-15
     * *
     * @param b 0-15
     */
    constructor(r: Int, g: Int, b: Int) {
        create(r, g, b)
    }

    /**
     * Create Col4096 colour and convert it to Slick Color
     * @param i
     * *
     * @return
     */
    override fun createSlickColor(raw: Int): Color {
        assertRaw(raw)

        val a: Int
        val r: Int
        val g: Int
        val b: Int

        r = raw and 0xF00 shr 8
        g = raw and 0x0F0 shr 4
        b = raw and 0x00F

        if (raw > 0xFFF) {
            a = raw and 0xF000 shr 12

            return Color(
                    r.shl(4) or r, g.shl(4) or g, b.shl(4) or b, a.shl(4) or a)
        }
        else {
            return Color(
                    r.shl(4) or r, g.shl(4) or g, b.shl(4) or b)
        }
    }

    override fun createSlickColor(r: Int, g: Int, b: Int): Color {
        assertARGB(0, r, g, b)
        return createSlickColor(r.shl(8) or g.shl(4) or b)
    }

    fun createSlickColor(a: Int, r: Int, g: Int, b: Int): Color {
        assertARGB(a, r, g, b)
        return createSlickColor(a.shl(12) or r.shl(8) or g.shl(4) or b)
    }

    override fun create(raw: Int) {
        assertRaw(raw)
        this.raw = (raw and 0xFFFF).toShort()
    }

    override fun create(r: Int, g: Int, b: Int) {
        assertARGB(0, r, g, b)
        raw = (r.shl(8) or g.shl(4) or b).toShort()
    }

    fun create(a: Int, r: Int, g: Int, b: Int) {
        assertARGB(a, r, g, b)
        raw = (a.shl(12) or r.shl(8) or g.shl(4) or b).toShort()
    }

    /**
     * Convert to 3 byte values, for raster imaging.
     * @return byte[RR, GG, BB] e.g. 0x4B3 -> 0x44, 0xBB, 0x33
     */
    fun toByteArray(): ByteArray {
        val ret = ByteArray(3)
        val r = (raw.toInt() and 0xF00) shr 8
        val g = (raw.toInt() and 0x0F0) shr 4
        val b = (raw.toInt() and 0x00F)

        ret[0] = (r.shl(4) or r).toByte()
        ret[1] = (g.shl(4) or g).toByte()
        ret[2] = (b.shl(4) or b).toByte()

        return ret
    }

    override fun toSlickColour(): Color = createSlickColor(raw.toUint())

    private fun assertRaw(i: Int) {
        if (i > 0xFFFF || i < 0) {
            println("i: " + i.toString())
            throw IllegalArgumentException()
        }
    }

    private fun assertARGB(a: Int, r: Int, g: Int, b: Int) {
        if (a !in 0..16 || r !in 0..16 || g !in 0..16 || b !in 0..16) {
            println("a: " + a.toString())
            println("r: " + r.toString())
            println("g: " + g.toString())
            println("b: " + b.toString())
            throw IllegalArgumentException()
        }
    }

    fun Short.toUint(): Int = this.toInt() and 0xFFFF
}
