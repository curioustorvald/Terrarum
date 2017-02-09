package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.terrarum.blendMul
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.peripheral.PeripheralVideoCard
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Created by SKYHi14 on 2017-02-08.
 */
class GraphicsTerminal(
        private val host: BaseTerrarumComputer, val videoCard: PeripheralVideoCard
) : Terminal {
    override val width = videoCard.termW
    override val height = videoCard.termH
    override val coloursCount = videoCard.coloursCount
    override var cursorX = 0
    override var cursorY = 0
    override var cursorBlink = true

    override var backColour = 15 // black
    override var foreColour = 48 // bright grey

    override var lastInputByte = -1

    override fun getColor(index: Int) = videoCard.CLUT[index]

    override val displayW = videoCard.width //+ 2 * borderSize
    override val displayH = videoCard.height //+ 2 * borderSize

    private val videoScreen = Image(videoCard.width, videoCard.height)

    override fun printChars(s: String) {
        TODO("not implemented")
    }

    override fun update(gc: GameContainer, delta: Int) {
        wrap()
    }

    // copied from SimpleTextTerminal
    private fun wrap() {
        // wrap cursor
        if (cursorX < 0 && cursorY <= 0) {
            setCursor(0, 0)
        }
        else if (cursorX >= width) {
            setCursor(0, cursorY + 1)
        }
        else if (cursorX < 0) {
            setCursor(width - 1, cursorY - 1)
        }
        // auto scroll up
        if (cursorY >= height) {
            scroll()
        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        videoCard.render(videoScreen.graphics)
        g.drawImage(videoScreen.getScaledCopy(2f), 0f, 0f)
    }

    override fun keyPressed(key: Int, c: Char) {
        TODO("not implemented")
    }

    override fun writeChars(s: String) {
        TODO("not implemented")
    }

    /** Unlike lua function, this one in Zero-based. */
    override fun setCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    override fun openInput(echo: Boolean) {
        TODO("not implemented")
    }

    override fun emitChar(bufferChar: Int, x: Int, y: Int) {
        TODO("not implemented")
    }

    override fun closeInputKey(keyFromUI: Int): Int {
        TODO("not implemented")
    }

    override fun closeInputString(): String {
        TODO("not implemented")
    }

    override var lastStreamInput: String? = null
    override var lastKeyPress: Int? = null

    override fun emitChar(c: Char, x: Int, y: Int) {
        TODO("not implemented")
    }

    override fun printChar(c: Char) {
        TODO("not implemented")
    }

    override fun emitString(s: String, x: Int, y: Int) {
        TODO("not implemented")
    }

    override fun printString(s: String, x: Int, y: Int) {
        TODO("not implemented")
    }

    override fun writeString(s: String, x: Int, y: Int) {
        TODO("not implemented")
    }

    override fun clear() {
        videoCard.clearAll()
    }

    override fun clearLine() {
        TODO("not implemented")
    }

    override fun newLine() {
        TODO("not implemented")
    }

    override fun scroll(amount: Int) {
        TODO("not implemented")
    }

    override fun setColour(back: Int, fore: Int) {
        TODO("not implemented")
    }

    override fun resetColour() {
        TODO("not implemented")
    }

    /**    // copied from SimpleTextTerminal
     * @param duration: milliseconds
     * @param freg: Frequency (float)
     */
    override fun emitTone(duration: Int, freq: Double) {
        host.clearBeepQueue()
        host.enqueueBeep(duration, freq)
    }

    // copied from SimpleTextTerminal
    /** for "emitTone code" on modern BIOS. */
    override fun bell(pattern: String) {
        host.clearBeepQueue()

        val freq: Double =
                if (host.luaJ_globals["computer"]["bellpitch"].isnil())
                    1000.0
                else
                    host.luaJ_globals["computer"]["bellpitch"].checkdouble()

        for (c in pattern) {
            when (c) {
                '.' -> { host.enqueueBeep(80, freq);  host.enqueueBeep(50, 0.0) }
                '-' -> { host.enqueueBeep(200, freq); host.enqueueBeep(50, 0.0) }
                '=' -> { host.enqueueBeep(500, freq); host.enqueueBeep(50, 0.0) }
                ' ' -> { host.enqueueBeep(200, 0.0) }
                ',' -> { host.enqueueBeep(50, 0.0) }
                else -> throw IllegalArgumentException("Unacceptable pattern: $c (from '$pattern')")
            }
        }
    }

    override fun getKeyPress(): Int? {
        TODO("not implemented")
    }
}