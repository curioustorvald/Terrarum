package net.torvald.terrarum.virtualcomputers.terminal

import net.torvald.aa.AAFrame
import net.torvald.aa.ColouredFastFont
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendMul
import org.newdawn.slick.*

/**
 * Default text terminal, four text colours (black, grey, lgrey, white).
 *
 * Created by minjaesong on 16-09-07.
 */
open class SimpleTextTerminal(
        val phosphor: Color, override val width: Int, override val height: Int
) : Terminal {
    override val coloursCount = 4

    private val colors = arrayOf(
            Color(0x19, 0x19, 0x19),
            Color(0xff, 0xff, 0xff),
            Color(0x55, 0x55, 0x55),
            Color(0xaa, 0xaa, 0xaa)
    )

    private val backDefault = 0
    private val foreDefault = colors.size - 1

    override var backColour = backDefault
    override var foreColour = foreDefault
    private val colourKey: Int
        get() = backColour.shl(4).plus(foreColour).and(0xFF)

    override var cursorX = 0
    override var cursorY = 0
    override var cursorBlink = true

    val screenBuffer = AAFrame(width, height)

    private val fontRef = "./assets/graphics/fonts/MDA.png"
    private val fontImg = Image(fontRef)
    private val fontW = fontImg.width / 16
    private val fontH = fontImg.height / 16
    private val font = ColouredFastFont(this, fontRef, fontW, fontH)

    override var isInputStreamOpen: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
        }

    override val displayW = fontW * width
    override val displayH = fontH * height

    private val TABSIZE = 4

    private val ASCII_NUL = 0.toChar()

    private val ASCII_BEL = 7.toChar()   // *BEEP!*
    private val ASCII_BS = 8.toChar()    // x = x - 1
    private val ASCII_TAB = 9.toChar()   // move cursor to next (TABSIZE * yy) pos (5 -> 8, 3- > 4, 4 -> 8)
    private val ASCII_LF = 10.toChar()   // new line
    private val ASCII_FF = 12.toChar()   // new page
    private val ASCII_CR = 13.toChar()   // x <- 0
    private val ASCII_DEL = 127.toChar() // backspace and delete char

    private var cursorBlinkTimer = 0
    private val cursorBlinkLen = 250
    private var cursorBlinkOn = true

    override fun getColor(index: Int): Color = colors[index]

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

        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val ch = screenBuffer.getChar(x, y)

                // background
                g.color = getColor(screenBuffer.getBackgroundColour(x, y))
                g.fillRect(fontW * x.toFloat(), fontH * y.toFloat(), fontW.toFloat(), fontH.toFloat())

                // foreground
                if (ch.toInt() != 0 && ch.toInt() != 32) {
                    g.color = getColor(screenBuffer.getForegroundColour(x, y))
                    g.drawString(
                            Character.toString(ch),
                            fontW * x.toFloat(), fontH * y.toFloat())
                }
            }
        }


        // cursor
        g.color = getColor(foreColour)
        if (cursorBlinkOn && cursorBlink)
            g.fillRect(
                    fontW * cursorX.toFloat(),
                    fontH * cursorY.toFloat(),
                    fontW.toFloat(),
                    fontH.toFloat()
            )



        // colour overlay
        g.color = phosphor
        blendMul()
        g.fillRect(0f, 0f, displayW.toFloat(), displayH.toFloat())


        blendNormal()

    }

    override fun setCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    /** Emits a bufferChar. Does not move cursor */
    override fun emitChar(bufferChar: Int) {
        screenBuffer.drawBuffer(cursorX, cursorY, bufferChar.toChar())
    }

    /** Emits a char. Does not move cursor */
    override fun emitChar(c: Char) {
        screenBuffer.drawBuffer(cursorX, cursorY, c.toInt().and(0xFF).toChar(), colourKey)
    }

    val asciiControlInUse = charArrayOf(
            ASCII_BEL,
            ASCII_BS,
            ASCII_TAB,
            ASCII_LF,
            ASCII_FF,
            ASCII_CR,
            ASCII_DEL
    )

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
                ASCII_LF  -> { cursorX = 0; cursorY += 1; wrap() }
                ASCII_FF  -> clear()
                ASCII_CR  -> { cursorX = 0 }
                ASCII_DEL -> { cursorX -= 1; wrap(); emitChar(colourKey.shl(8)) }
            }
        }
    }

    /** Emits a string and move cursor accordingly. */
    override fun printString(s: String, x: Int, y: Int) {
        setCursor(x, y)
        emitString(s)
        val absCursorPos = cursorX + cursorY * width + s.length
        setCursor(x % width, y / width)
    }

    /** Emits a string. Does not move cursor */
    override fun emitString(s: String) {
        val x = cursorX
        val y = cursorY

        for (i in 0..s.length - 1)
            printChar(s[i])

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

    override fun scroll(amount: Int) {
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

    override fun beep(freq: Int, duration: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun isOOB(x: Int, y: Int) =
            (x < 0 || y < 0 || x >= width || y >= height)

    companion object {
        val AMBER = Color(255, 183, 0) // P3, 602 nm
        val IBM_GREEN = Color(74, 255, 0) // P39, 525 nm
        val WHITE = Color(228, 234, 255) // P4, 7 500 K
        val ELECTRIC_BLUE = Color(0, 239, 255) // imaginary, 486 nm
        val RED = Color(250, 0, 0) // <= 645 nm
    }

}