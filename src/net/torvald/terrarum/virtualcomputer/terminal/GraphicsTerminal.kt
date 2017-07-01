package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.terrarum.blendMul
import net.torvald.terrarum.gameactors.DecodeTapestry
import net.torvald.terrarum.gameactors.abs
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import java.util.*

/**
 * Printing text using Term API triggers 'compatibility' mode, where you are limited to 16 colours.
 * Use PPU API for full 64 colours!
 *
 * Created by SKYHi14 on 2017-02-08.
 */
/*class GraphicsTerminal(private val host: TerrarumComputer) : Terminal {
    lateinit var videoCard: PeripheralVideoCard

    override val width: Int; get() = videoCard.termW
    override val height: Int; get() = videoCard.termH
    override val coloursCount: Int; get() = videoCard.colorsCount
    override var cursorX = 0
    override var cursorY = 0
    override var cursorBlink = true

    val backDefault = 0 // black
    val foreDefault = 1 // white

    override var backColour = backDefault
    override var foreColour = foreDefault

    private val colourKey: Int
        get() = backColour.shl(4) or (foreColour).and(0xFF)

    override fun getColor(index: Int) = videoCard.CLUT[index]

    override val displayW: Int; get() = videoCard.width //+ 2 * borderSize
    override val displayH: Int; get() = videoCard.height //+ 2 * borderSize

    private lateinit var videoScreen: Image

    var TABSIZE = 4

    val errorColour = 6

    override fun printChars(s: String) {
        printString(s, cursorX, cursorY)
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
        videoCard.render(g)
    }

    override fun keyPressed(key: Int, c: Char) {
        host.keyPressed(key, c)
    }

    override fun writeChars(s: String) {
        writeString(s, cursorX, cursorY)
    }

    /** Unlike lua function, this one in Zero-based. */
    override fun setCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    override fun emitChar(bufferChar: Int, x: Int, y: Int) {
        videoCard.drawChar(
                bufferChar.and(0xFF).toChar(),
                x * PeripheralVideoCard.blockW,
                y * PeripheralVideoCard.blockH,
                CLUT16_TO_64[bufferChar.ushr(8).and(0xF)],
                CLUT16_TO_64[bufferChar.ushr(12).and(0xF)]
        )
    }

    override fun emitChar(c: Char, xx: Int, yy: Int) {
        wrap() // needed
        var nx = xx
        var ny = yy
        // wrap argument cursor
        if (xx < 0 && yy <= 0) {
            nx = 0
            ny = 0
        }
        else if (cursorX >= width) {
            println("arstenioarstoneirastneo")
            nx = 0
            ny += 1
        }
        else if (cursorX < 0) {
            nx = width - 1
            ny -= 1
        }
        // auto scroll up
        if (cursorY >= height) {
            scroll()
            ny -= 1
        }

        println("xx: $xx, yy: $yy")
        println("nx: $nx, ny: $ny")

        videoCard.drawChar(
                c,
                nx * PeripheralVideoCard.blockW,
                ny * PeripheralVideoCard.blockH,
                CLUT16_TO_64[foreColour]
        )
    }

    override fun printChar(c: Char) {
        if (c >= ' ' && c.toInt() != 127) {
            emitChar(c, cursorX, cursorY)
            cursorX += 1
        }
        else {
            if (BACKSPACE.contains(c)) {
                cursorX -= 1
                //wrap()
                emitChar(0.toChar(), cursorX, cursorY)
            }
            else {
                when (c) {
                    ASCII_BEL -> bell(".")
                    ASCII_TAB -> { cursorX = (cursorX).div(TABSIZE).times(TABSIZE) + TABSIZE }
                    ASCII_LF  -> { newLine(); System.err.println("LF ${Random().nextInt(100)}") }
                    ASCII_FF  -> clear()
                    ASCII_CR  -> { cursorX = 0 }
                    ASCII_DC1, ASCII_DC2, ASCII_DC3,
                    ASCII_DC4 -> { foreColour = c - ASCII_DC1 }
                    ASCII_DLE -> { foreColour = errorColour }
                }
            }
        }
    }

    override fun emitString(s: String, x: Int, y: Int) {
        setCursor(x, y)

        for (i in 0..s.length - 1) {
            printChar(s[i])
        }

        setCursor(x, y)
    }

    override fun printString(s: String, x: Int, y: Int) {
        writeString(s, x, y)
        newLine()
    }

    override fun writeString(s: String, x: Int, y: Int) {
        setCursor(x, y)

        for (i in 0..s.length - 1) {
            printChar(s[i])
        }
    }

    override fun clear() {
        videoCard.clearForeground()
    }

    override fun clearLine() {
        //TODO("not implemented")
    }

    override fun newLine() {
        cursorX = 0; cursorY += 1
        //wrap()
    }

    override fun scroll(amount: Int) {
        val rgba = videoCard.vram.foreground.rgba
        val displacement = amount.abs() * PeripheralVideoCard.blockH * videoCard.vram.foreground.texWidth * 4
        if (amount >= 0) {
            System.arraycopy(
                    rgba, displacement,
                    rgba, 0,
                    rgba.size - displacement
            )
            for (it in rgba.size - 1 downTo rgba.size - displacement + 1) { rgba[it] = 0.toByte() }
        }
        else {
            System.arraycopy(
                    rgba, 0,
                    rgba, displacement,
                    rgba.size - displacement
            )
            for (it in 0..displacement - 1) { rgba[it] = 0.toByte() }
        }

        cursorY += -amount
    }

    /**
     * does not changes color setting in PPU
     */
    override fun setColour(back: Int, fore: Int) {
        foreColour = fore
        backColour = back
    }

    /**
     * does not changes color setting in PPU
     */
    override fun resetColour() {
        foreColour = foreDefault
        backColour = backDefault
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

    companion object {
        private val WHITE7500 = Color(0xe4eaff)

        val ASCII_NUL = 0.toChar()
        val ASCII_BEL = 7.toChar()   // *BEEP!*
        val ASCII_TAB = 9.toChar()   // move cursor to next (TABSIZE * yy) pos (5 -> 8, 3- > 4, 4 -> 8)
        val ASCII_LF = 10.toChar()   // new line
        val ASCII_FF = 12.toChar()   // new page
        val ASCII_CR = 13.toChar()   // x <- 0
        val BACKSPACE = arrayOf(127.toChar(), 8.toChar()) // backspace and delete char (8 for WIN, 127 for OSX)
        val ASCII_DC1 = 17.toChar()  // foreground colour 0
        val ASCII_DC2 = 18.toChar()  // foreground colour 1
        val ASCII_DC3 = 19.toChar()  // foreground colour 2
        val ASCII_DC4 = 20.toChar()  // foreground colour 3
        val ASCII_DLE = 16.toChar()  // error message colour

        val asciiControlInUse = charArrayOf(
                ASCII_NUL,
                ASCII_BEL,
                8.toChar(),
                ASCII_TAB,
                ASCII_LF,
                ASCII_FF,
                ASCII_CR,
                127.toChar(),
                ASCII_DC1,
                ASCII_DC2,
                ASCII_DC3,
                ASCII_DC4,
                ASCII_DLE
        )

        val CLUT = DecodeTapestry.colourIndices64
        val CLUT16_TO_64 = intArrayOf(
                15, 49, 16, 48, 44, 29, 33, 18,
                 5, 22, 39, 26, 25, 10, 31, 13
        )
    }

    fun attachVideoCard(videocard: PeripheralVideoCard) {
        this.videoCard = videocard

        videoScreen = Image(videoCard.width, videoCard.height)
    }
}*/