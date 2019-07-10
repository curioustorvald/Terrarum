package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import net.torvald.UnsafeHelper
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameworld.fmod
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.Charset

/**
 * Only one kind of display adapter should exist in the game: they add nothing to the game and the game
 * shouldn't put much emphasis on computers anyway.
 *
 * The actual draw code must not exist in this class!
 *
 * Created by minjaesong on 2019-07-09.
 */
class MDA(val width: Int, val height: Int) {

    companion object {
        val charset: Charset = Charset.forName("cp437")
    }

    init {
        if (width % 2 == 1) throw IllegalArgumentException("Display width must be an even number (width = $width)")
    }

    private val arrayElemOffset = 8L * if (AppLoader.is32BitJVM) 1 else 2 // 8 for 32-bit, 16 for 64-bit

    private val glyphs = UnsafeHelper.allocate(width.toLong() * height + 1) // extra one byte is absolutely needed
    private val attributes = UnsafeHelper.allocate(width.toLong() * height + 1)

    var cursor = 0
        private set
    var background = 0
    var foreground = 1
    var blink = true

    init {
        //glyphs.fillWith(0)
        //attributes.fillWith(1)
    }

    /*
    Attibutes memory map:

    for every byte:

    (msb) 00bb 00ff (lsb)

    where:

    bb: background colour
    ff: foreground colour

    Colours:

    0: black
    1: light grey
    2: dark grey
    3: white

     */

    fun toAttribute(back: Int, fore: Int) = (back.shl(4) or fore).toByte()

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

    private fun set(offset: Int, glyph: Byte, attribute: Byte) {
        glyphs[offset.toLong()] = glyph
        attributes[offset.toLong()] = attribute
    }

    fun setCursor(x: Int, y: Int) {
        cursor = wrapAround(x, y).toAddress().toInt()
    }

    // SETTEXT methods should not deal with the scrolling, it must be handled by the PRINT methods.

    /** Bulk write method. Any control characers will be represented as a glyph, rather than an actual control sequence.
     * E.g. '\n' will print a symbol. */
    fun setText(x: Int, y: Int, text: ByteArray, attribute: ByteArray) {
        UnsafeHelper.memcpyRaw(text, arrayElemOffset, null, glyphs.ptr + wrapAround(x, y).toAddress(), text.size.toLong())
        UnsafeHelper.memcpyRaw(attribute, arrayElemOffset, null, attributes.ptr + wrapAround(x, y).toAddress(), text.size.toLong())
    }

    private fun setText(offset: Int, text: ByteArray, attribute: ByteArray) {
        UnsafeHelper.memcpyRaw(text, arrayElemOffset, null, glyphs.ptr + offset, text.size.toLong())
        UnsafeHelper.memcpyRaw(attribute, arrayElemOffset, null, attributes.ptr + offset, text.size.toLong())
    }

    /** Bulk write method. Any control characers will be represented as a glyph, rather than an actual control sequence.
     * E.g. '\n' will print a symbol. */
    fun setText(x: Int, y: Int, text: ByteArray, attribute: Byte) {
        setText(x, y, text, ByteArray(text.size) { attribute })
    }

    private fun setText(offset: Int, text: ByteArray, attribute: Byte = toAttribute(background, foreground)) {
        setText(offset, text, ByteArray(text.size) { attribute })
    }

    /** Bulk write method. Any control characers will be represented as a glyph, rather than an actual control sequence.
     * E.g. '\n' will print a symbol. */
    inline fun setText(x: Int, y: Int, text: ByteArray) {
        setText(x, y, text, toAttribute(background, foreground))
    }

    fun setOneText(x: Int, y: Int, text: Byte, attribute: Byte = toAttribute(background, foreground)) {
        val o = wrapAround(x, y).toAddress()
        glyphs[o] = text
        attributes[o] = attribute
    }

    private fun setOneText(offset: Int, text: Byte, attribute: Byte = toAttribute(background, foreground)) {
        glyphs[offset.toLong()] = text
        attributes[offset.toLong()] = attribute
    }



    fun println(text: String) {
        print(text)
        write(0x0A)
    }
    inline fun print(text: String) {
        print(text.toByteArray(charset))
    }

    fun println(text: ByteArray) {
        print(text)
        write(0x0A)
    }
    fun print(text: ByteArray) {
        text.forEach { write(it) }
    }

    fun write(text: Byte) {
        when (text) {
            // LF
            0x0A.toByte() -> newline()
            // all others (e.g. CR)
            in 0x00.toByte()..0x0D.toByte() -> { /* do nothing */ }

            else -> {
                setOneText(cursor, text)
                cursor += 1

                if (cursor > width * height) {
                    scroll(1)
                }
            }
        }

    }

    fun clear() {
        glyphs.fillWith(0)
        attributes.fillWith(toAttribute(background, foreground))
        cursor = 0
    }

    fun clearCurrentLine() {
        clearLine(cursor / width)
    }

    fun clearLine(line: Int) {
        val lineOffset = line * width
        for (i in 0L until width) {
            glyphs[lineOffset + i] = 0
        }
    }

    fun clearLineAfterCursor() {
        val lineOffset = (cursor / width) * width
        for (i in (cursor % width).toLong() until width) {
            glyphs[lineOffset + i] = 0
        }
    }

    /**
     * moves text and the current cursor position
     */
    fun scroll(amount: Int) {
        val offset = (width * amount).toLong()
        if (amount < 0) throw IllegalArgumentException("amount = $amount")

        UnsafeHelper.memcpy(glyphs, offset, glyphs, 0L, glyphs.size - offset)
        UnsafeHelper.memcpy(attributes, offset, attributes, 0L, attributes.size - offset)

        cursor -= offset.toInt()
        if (cursor < 0) cursor = 0

        clearLineAfterCursor()
    }

    /**
     * Advance one line, scroll the screen if necessary
     */
    fun newline() {
        cursor += width

        if (cursor >= width * height) {
            scroll(1)
        }

        cursor = (cursor / width) * width // set cursorX to 0
        clearLineAfterCursor()
    }

    fun dispose() {
        glyphs.destroy()
        attributes.destroy()
    }
}

private class MDAOutputStream(val mda: MDA) : OutputStream() {
    override fun write(b: Int) {
        mda.write(b.toByte())
    }
}

class MDAPrintStream(val mda: MDA) : PrintStream(MDAOutputStream(mda)) {
    override fun print(s: String?) {
        mda.print((s ?: "").toByteArray(MDA.charset))
    }

    override fun println(s: String?) {
        print(s)
        mda.newline()
    }
}