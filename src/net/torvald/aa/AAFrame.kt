package net.torvald.aa

import net.torvald.terrarum.gameworld.toUint
import net.torvald.terrarum.virtualcomputer.terminal.SimpleTextTerminal
import org.newdawn.slick.*

/**
 * @param terminal: for sending redraw only
 *
 * Created by minjaesong on 16-08-10.
 */
class AAFrame @Throws(SlickException::class)
constructor(var width: Int, var height: Int, var terminal: SimpleTextTerminal) {

    /**
     * 0000_0000_00000000

     * Upper bits:  Background colour
     *
     * Middle bits: Foreground colour
     *
     * Lower 8 bits: CP437
     */
    internal val frameBuffer: CharArray

    val sizeof = 2 * width * height // magic number 2: indicator that we're using char

    init {
        frameBuffer = CharArray(width * height)
    }

    fun drawBuffer(x: Int, y: Int, c: Char, colourKey: Int) {
        if (y * width + x >= frameBuffer.size || y * width + x < 0)
            throw ArrayIndexOutOfBoundsException("x: $x, y; $y")
        frameBuffer[y * width + x] = ((c.toInt().and(0xFF)) + colourKey.shl(8)).toChar()

        terminal.redraw()
    }

    fun drawBuffer(x: Int, y: Int, raw: Char): Boolean =
        if (checkOOB(x, y))
            false
        else {
            frameBuffer[y * width + x] = raw
            terminal.redraw()
            true
        }

    fun drawFromBytes(other: ByteArray) {
        for (i in 0..other.size - 1 step 2) {
            val char = (other[i].toUint().shl(8) + other[i + 1].toUint()).toChar()
            frameBuffer[i.ushr(1)] = char
        }
        terminal.redraw()
    }

    fun getBackgroundColour(x: Int, y: Int): Int {
        return frameBuffer[y * width + x].toInt().ushr(12) and 0xF
    }

    fun getForegroundColour(x: Int, y: Int): Int {
        return frameBuffer[y * width + x].toInt().ushr(8) and 0xF
    }

    fun getChar(x: Int, y: Int): Char {
        return (frameBuffer[y * width + x].toInt() and 0xFF).toChar()
    }

    fun getRaw(x: Int, y: Int): Char? =
        if (checkOOB(x, y))
            null
        else
            frameBuffer[y * width + x]

    fun clear(background: Int = 0) {
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                drawBuffer(x, y, 0.toChar(), background.shl(4))
            }
        }
        terminal.redraw()
    }

    fun drawFromOther(other: AAFrame) {
        //this.framebuffer = other.getFrameBuffer();
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                frameBuffer[y * width + x] = other.getRaw(x, y)!!
            }
        }
        terminal.redraw()
    }

    private fun checkOOB(x: Int, y: Int) = (x < 0 || y < 0 || x >= width || y >= height)

    fun getColourKey(x: Int, y: Int): Int = frameBuffer[y * width + x].toInt().ushr(8).and(0xFF)
}
