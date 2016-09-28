package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.aa.AAFrame
import net.torvald.aa.ColouredFastFont
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendMul
import net.torvald.terrarum.blendScreen
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import org.newdawn.slick.*
import java.nio.ByteBuffer
import java.util.*

/**
 * Default text terminal, four text colours (black, grey, lgrey, white).
 *
 * Created by minjaesong on 16-09-07.
 */
open class SimpleTextTerminal(
        phosphorColour: Color, override val width: Int, override val height: Int, private val host: BaseTerrarumComputer,
        colour: Boolean = false, hires: Boolean = false
) : Terminal {

    private val DEBUG = false

    /**
     * Terminals must support AT LEAST 4 colours.
     * Color index 0 must be default background, index 3 must be default foreground
     */
    open protected val colours = if (colour)
        arrayOf(
                Color(0x00, 0x00, 0x00), // 0 black
                Color(0xff, 0xff, 0xff), // 1 white
                Color(0x55, 0x55, 0x55), // 2 dim grey
                Color(0xaa, 0xaa, 0xaa), // 3 bright grey

                Color(0xff, 0xff, 0x00), // 4 yellow
                Color(0xff, 0x66, 0x00), // 5 orange
                Color(0xdd, 0x00, 0x00), // 6 red
                Color(0xff, 0x00, 0x99), // 7 magenta

                Color(0x33, 0x00, 0x99), // 8 purple
                Color(0x00, 0x00, 0xcc), // 9 blue
                Color(0x00, 0x99, 0xff), //10 cyan
                Color(0x55, 0xff, 0x00), //11 lime

                Color(0x00, 0xaa, 0x00), //12 green
                Color(0x00, 0x66, 0x00), //13 dark green
                Color(0x66, 0x33, 0x00), //14 brown
                Color(0x99, 0x66, 0x33)  //15 tan
        )                                // THESE ARE THE STANDARD
    else
        arrayOf(
                Color(0x00, 0x00, 0x00), // black
                Color(0xff, 0xff, 0xff), // white
                Color(0x55, 0x55, 0x55), // dim grey
                Color(0xaa, 0xaa, 0xaa)  // light grey
        )                                // THESE ARE THE STANDARD

    val phosphor = if (colour) WHITE7500 else phosphorColour
    open protected val colourScreen = if (colour) Color(8, 8, 8) else Color(19, 19, 19)

    override val coloursCount: Int
        get() = colours.size

    val errorColour = if (coloursCount > 4) 6 else 1

    open protected val backDefault = 0 // STANDARD
    open protected val foreDefault = 3 // STANDARD

    override var backColour = backDefault
    override var foreColour = foreDefault
    private val colourKey: Int
        get() = backColour.shl(4).plus(foreColour).and(0xFF)

    override var cursorX = 0
    override var cursorY = 0
    override var cursorBlink = true

    val screenBuffer = AAFrame(width, height)

    open protected val fontRef =
            "./assets/graphics/fonts/${
                if (hires) "milky.png"
                else if (phosphor == GREEN || phosphor == AMBER) "MDA.png"
                else "milkymda.png"
            }"
    open protected val fontImg = Image(fontRef)
    open protected val fontW = fontImg.width / 16
    open protected val fontH = fontImg.height / 16
    open protected val font = ColouredFastFont(this, fontRef, fontW, fontH)

    private val borderSize = 20
    override val displayW = fontW * width + 2 * borderSize
    override val displayH = fontH * height + 2 * borderSize


    var TABSIZE = 4

    private var cursorBlinkTimer = 0
    private val cursorBlinkLen = 250
    private var cursorBlinkOn = true



    override fun getColor(index: Int): Color = colours[index]

    override fun update(gc: GameContainer, delta: Int) {
        cursorBlinkTimer = cursorBlinkTimer.plus(delta)
        if (cursorBlinkTimer > cursorBlinkLen) {
            cursorBlinkTimer -= cursorBlinkLen
            cursorBlinkOn = !cursorBlinkOn
        }

        wrap()
    }

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

    /**
     * pass UIcanvas to the parameter "g"
     */
    override fun render(gc: GameContainer, g: Graphics) {
        g.font = font

        blendNormal()

        // black background (this is mandatory)
        g.color = Color.black
        g.fillRect(0f, 0f, displayW.toFloat(), displayH.toFloat())


        // screen buffer
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val ch = screenBuffer.getChar(x, y)

                // background
                g.color = getColor(screenBuffer.getBackgroundColour(x, y))
                g.fillRect(fontW * x.toFloat() + borderSize, fontH * y.toFloat() + borderSize,
                        fontW.toFloat(), fontH.toFloat())

                // foreground
                if (ch.toInt() != 0 && ch.toInt() != 32) {
                    g.color = getColor(screenBuffer.getForegroundColour(x, y))
                    g.drawString(
                            Character.toString(ch),
                            fontW * x.toFloat() + borderSize, fontH * y.toFloat() + borderSize)
                }
            }
        }


        // cursor
        g.color = getColor(foreDefault)
        if (cursorBlinkOn && cursorBlink)
            g.fillRect(
                    fontW * cursorX.toFloat() + borderSize,
                    fontH * cursorY.toFloat() + borderSize,
                    fontW.toFloat(),
                    fontH.toFloat()
            )


        // not-pure-black screen
        g.color = colourScreen
        blendScreen()
        g.fillRect(0f, 0f, displayW.toFloat(), displayH.toFloat())


        // colour overlay
        g.color = phosphor
        blendMul()
        g.fillRect(0f, 0f, displayW.toFloat(), displayH.toFloat())


        blendNormal()

    }

    /** Unlike lua function, this one in Zero-based. */
    override fun setCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    /** Emits a bufferChar. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    override fun emitChar(bufferChar: Int, x: Int, y: Int) {
        screenBuffer.drawBuffer(x, y, bufferChar.toChar())
    }

    /** Emits a char. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    override fun emitChar(c: Char, x: Int, y: Int) {
        screenBuffer.drawBuffer(x, y, c.toInt().and(0xFF).toChar(), colourKey)
    }

    /** Prints a char and move cursor accordingly. */
    override fun printChar(c: Char) {
        wrap()
        if (c >= ' ' && c.toInt() != 127) {
            emitChar(c)
            cursorX += 1
        }
        else {
            when (c) {
                ASCII_BEL -> beep()
                ASCII_BS  -> { cursorX -= 1; wrap() }
                ASCII_TAB -> { cursorX = (cursorX).div(TABSIZE).times(TABSIZE) + TABSIZE }
                ASCII_LF  -> newLine()
                ASCII_FF  -> clear()
                ASCII_CR  -> { cursorX = 0 }
                ASCII_DEL -> { cursorX -= 1; wrap(); emitChar(colourKey.shl(8)) }
                ASCII_DC1, ASCII_DC2, ASCII_DC3, ASCII_DC4 -> { foreColour = c - ASCII_DC1 }
                ASCII_DLE -> { foreColour = errorColour }
            }
        }
    }

    /** (TTY): Prints a series of chars and move cursor accordingly, then LF
     * (term): printString() on current cursor pos */
    override fun printChars(s: String) {
        printString(s, cursorX, cursorY)
    }

    /** (TTY): Prints a series of chars and move cursor accordingly
     * (term): writeString() on current cursor pos */
    override fun writeChars(s: String) {
        writeString(s, cursorX, cursorY)
    }

    /** Emits a string and move cursor accordingly, then do LF */
    override fun printString(s: String, x: Int, y: Int) {
        writeString(s, x, y)
        newLine()
    }

    /** Emits a string and move cursor accordingly. */
    override fun writeString(s: String, x: Int, y: Int) {
        setCursor(x, y)

        for (i in 0..s.length - 1) {
            printChar(s[i])
            wrap()
        }
    }

    /** Emits a string, does not affected by control sequences. Does not move cursor */
    override fun emitString(s: String, x: Int, y: Int) {
        setCursor(x, y)

        for (i in 0..s.length - 1) {
            printChar(s[i])
            wrap()
        }

        setCursor(x, y)
    }

    override fun clear() {
        screenBuffer.clear(backColour)
        cursorX = 0
        cursorY = 0
    }

    override fun clearLine() {
        for (i in 0..width - 1)
            screenBuffer.drawBuffer(i, cursorY, 0.toChar(), colourKey)
    }

    override fun newLine() {
        cursorX = 0; cursorY += 1; wrap()
    }

    override fun scroll(amount: Int) {
        if (amount < 0) throw IllegalArgumentException("cannot scroll up")

        val offset = amount * width
        for (i in offset..screenBuffer.sizeof.ushr(1) - 1) {
            screenBuffer.frameBuffer[i - offset] = screenBuffer.frameBuffer[i]
        }
        for (c in 1..amount) {
            cursorY -= 1
            clearLine()
        }
    }

    override fun setColour(back: Int, fore: Int) {
        backColour = back
        foreColour = fore
    }

    override fun resetColour() {
        backColour = backDefault
        foreColour = foreDefault
    }

    /**
     * @param duration: milliseconds
     * @param freg: Frequency (float)
     */
    override fun beep(duration: Int, freq: Float) {
        // println("!! Beep playing row $beepCursor, d ${Math.min(duration, maxDuration)} f $freq")
        host.clearBeepQueue()
        host.enqueueBeep(duration, freq)
    }

    /** for "beep code" on modern BIOS. */
    override fun bell(pattern: String) {
        host.clearBeepQueue()

        val freq: Float =
                if (host.luaJ_globals["computer"]["bellpitch"].isnil())
                    1000f
                else
                    host.luaJ_globals["computer"]["bellpitch"].checkdouble().toFloat()

        for (c in pattern) {
            when (c) {
                '.' -> { host.enqueueBeep(50, freq);  host.enqueueBeep(50, 0f) }
                '-' -> { host.enqueueBeep(200, freq); host.enqueueBeep(50, 0f) }
                '=' -> { host.enqueueBeep(500, freq); host.enqueueBeep(50, 0f) }
                ' ' -> { host.enqueueBeep(200, 0f) }
                ',' -> { host.enqueueBeep(50, 0f) }
                else -> throw IllegalArgumentException("Unacceptable pattern: $c (from '$pattern')")
            }
        }
    }

    override var lastInputByte: Int = -1
    var sb: StringBuilder = StringBuilder()
    private var inputOpen = false
    private var keyPressVisible = false
    /**
     * @param echo if true, keypresses are echoed to the terminal.
     */
    override fun openInput(echo: Boolean) {
        lastStreamInput = null
        lastKeyPress = null
        inputOpen = true
        keyPressVisible = echo
        if (DEBUG) println("[SimpleTextTerminal] openInput()")
    }

    override fun closeInputKey(keyFromUI: Int): Int {
        inputOpen = false
        lastKeyPress = keyFromUI

        if (DEBUG) println("[SimpleTextTerminal] closeInputKey(), $keyFromUI")
        return keyFromUI
    }

    override var lastStreamInput: String? = null
    override var lastKeyPress: Int? = null

    override fun closeInputString(): String {
        inputOpen = false
        lastStreamInput = sb.toString()
        sb = StringBuilder()

        if (DEBUG)
            println("[SimpleTextTerminal] closeInputString(), ${if (keyPressVisible) lastStreamInput else "<keypress hidden>"}")
        return lastStreamInput!!
    }

    override fun keyPressed(key: Int, c: Char) {
        lastInputByte = c.toInt()

        if (inputOpen) {
            if (c == ASCII_CR)
                printChar(ASCII_LF)
            else if (keyPressVisible)
                printChar(c)
            if (!asciiControlInUse.contains(c)) sb.append(c)
            else if (c == ASCII_DEL && sb.length > 0) sb.deleteCharAt(sb.length - 1)
        }
    }

    override fun getKeyPress(): Int? = lastInputByte

    private fun isOOB(x: Int, y: Int) =
            (x < 0 || y < 0 || x >= width || y >= height)

    companion object {
        val AMBER = Color(255, 183, 0) // P3, 602 nm
        val GREEN = Color(74, 255, 0) // P39, 525 nm
        val WHITE = Color(204, 223, 255) // approximation of white CRT I own
        private val WHITE7500 = Color(0xe4eaff)
        val BLUE_NOVELTY = Color(0, 226, 255) // imaginary, 483 nm
        val RED = Color(250, 51, 0) // 632 nm

        val ASCII_NUL = 0.toChar()
        val ASCII_BEL = 7.toChar()   // *BEEP!*
        val ASCII_BS = 8.toChar()    // x = x - 1
        val ASCII_TAB = 9.toChar()   // move cursor to next (TABSIZE * yy) pos (5 -> 8, 3- > 4, 4 -> 8)
        val ASCII_LF = 10.toChar()   // new line
        val ASCII_FF = 12.toChar()   // new page
        val ASCII_CR = 13.toChar()   // x <- 0
        val ASCII_DEL = 127.toChar() // backspace and delete char
        val ASCII_DC1 = 17.toChar()  // foreground colour 0
        val ASCII_DC2 = 18.toChar()  // foreground colour 1
        val ASCII_DC3 = 19.toChar()  // foreground colour 2
        val ASCII_DC4 = 20.toChar()  // foreground colour 3
        val ASCII_DLE = 16.toChar()  // error message colour

        val asciiControlInUse = charArrayOf(
                ASCII_NUL,
                ASCII_BEL,
                ASCII_BS,
                ASCII_TAB,
                ASCII_LF,
                ASCII_FF,
                ASCII_CR,
                ASCII_DEL,
                ASCII_DC1,
                ASCII_DC2,
                ASCII_DC3,
                ASCII_DC4,
                ASCII_DLE
        )
    }
}

class ALException(errorCode: Int) : Exception("ALerror: $errorCode") {

}
