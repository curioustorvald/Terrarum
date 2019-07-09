package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import net.torvald.UnsafeHelper
import net.torvald.terrarum.gameworld.fmod

/**
 * Only one kind of display adapter should exist in the game: they add nothing to the game and the game
 * shouldn't put much emphasis on computers anyway.
 *
 * The actual draw code must not exist in this class!
 *
 * Created by minjaesong on 2019-07-09.
 */
class MDA(val width: Int, val height: Int) {

    init {
        if (width % 2 == 1) throw IllegalArgumentException("Display width must be an even number (width = $width)")
    }

    private val glyphs = UnsafeHelper.allocate(width.toLong() * height)
    private val attributes = UnsafeHelper.allocate(width.toLong() * height)

    var cursor = 0
        private set
    var background = 0
    var foreground = 1
    var blink = true

    init {
        glyphs.fillWith(0)
        attributes.fillWith(1)
    }

    /*
    Attibutes memory map:

    for every byte:

    (msb) 0000 bbff (lsb)

    where:

    bb: background colour
    ff: foreground colour

    Colours:

    0: black
    1: light grey
    2: dark grey
    3: white

     */

    private fun wrapAround(x: Int, y: Int) = (x fmod width) to (y fmod height)
    private fun toAddress(x: Int, y: Int) = (y * width + x).toLong()
    inline private fun Pair<Int, Int>.toAddress() = toAddress(this.first, this.second)

    fun rawGet(offset: Int) = glyphs[offset.toLong()] to attributes[offset.toLong()]

    fun get(x: Int, y: Int): Pair<Byte, Byte> {
        val a = wrapAround(x, y).toAddress()
        return glyphs[a] to attributes[a]
    }

    fun set(x: Int, y: Int, glyph: Byte, attribute: Byte) {
        val a = wrapAround(x, y).toAddress()
        glyphs[a] = glyph
        attributes[a] = attribute
    }

    fun setCursor(x: Int, y: Int) {
        cursor = wrapAround(x, y).toAddress().toInt()
    }


    fun setText(x: Int, y: Int, text: ByteArray, attirbute: ByteArray) {
        UnsafeHelper.memcpyRaw(text, 0, null, glyphs.ptr + wrapAround(x, y).toAddress(), text.size.toLong())
        UnsafeHelper.memcpyRaw(attirbute, 0, null, attributes.ptr + wrapAround(x, y).toAddress(), text.size.toLong())
    }

    fun setText(x: Int, y: Int, text: ByteArray, attribute: Byte) {
        setText(x, y, text)
        val a = wrapAround(x, y).toAddress()
        for (i in 0 until text.size) {
            attributes[a + i] = attribute
        }
    }

    fun setText(x: Int, y: Int, text: ByteArray) {
        setText(x, y, text, (background.shl(0b11) or foreground).toByte())
    }



    fun dispose() {
        glyphs.destroy()
        attributes.destroy()
    }
}