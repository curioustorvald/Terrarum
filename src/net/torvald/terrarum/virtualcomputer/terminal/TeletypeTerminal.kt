package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.blendMul
import net.torvald.terrarum.blendNormal
import org.newdawn.slick.*
import java.util.*

/**
 * Created by minjaesong on 16-09-15.
 */
class TeletypeTerminal : Teletype {
    override val width = 40
    override val displayW: Int
        get() = width * font.W
    /**
     * 0: Teletype
     * 4: Non-colour terminal (Note that '2' is invalid!)
     * >4: Colour terminal
     */
    override val coloursCount = 0

    override var cursorX = 0

    private val font = TTYFont()

    private var lineBuffer = StringBuilder()

    private var currentJob = 0
    private var currentJobStep = 0
    private var currentJobLen = 0
    private var currentJobQueue: Any? = null
    // make sure certain jobs deliberately take long time by doing them one-by-one, for each frame.
    private val JOB_IDLE = 0
    private val JOB_MOVEHEAD = 1
    private val JOB_PRINTCHAR = 2
    private val JOB_LINEFEED = 3

    override fun update(gc: GameContainer, delta: Int) {
        wrap()

        /*when (currentJob) {
            JOB_PRINTCHAR -> {
                printChar((currentJobQueue!! as String)[currentJobStep])
                currentJobStep += 1
            }
            JOB_LINEFEED -> {
                newLine()
                currentJobStep += 1
            }
        }*/

        if (currentJobStep > currentJobLen) {
            currentJob = JOB_IDLE
            currentJobLen = 0
            currentJobStep = 0
            currentJobQueue = null
        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.font = font
        g.drawString(lineBuffer.toString(), 0f, 0f)
    }

    val TABSIZE = 4

    /** Prints a char and move cursor accordingly */
    override fun printChar(c: Char) {
        wrap()
        if (c >= ' ' && c.toInt() != 127) {
            lineBuffer.append(c)
            cursorX += 1
        }
        else {
            when (c) {
                SimpleTextTerminal.ASCII_BEL -> bell()
                SimpleTextTerminal.ASCII_BS  -> { cursorX -= 1; wrap() }
                SimpleTextTerminal.ASCII_TAB -> { cursorX = (cursorX).div(TABSIZE).times(TABSIZE) + TABSIZE }
                SimpleTextTerminal.ASCII_LF  -> newLine()
                SimpleTextTerminal.ASCII_FF  -> newLine()
                SimpleTextTerminal.ASCII_CR  -> { cursorX = 0 }
                SimpleTextTerminal.ASCII_DEL -> { } // NOT supported, do nothing
                SimpleTextTerminal.ASCII_DC1, SimpleTextTerminal.ASCII_DC2, SimpleTextTerminal.ASCII_DC3, SimpleTextTerminal.ASCII_DC4
                -> { } // NOT supported, do nothing
            }
        }
    }

    /** (TTY): Prints a series of chars and move cursor accordingly
     * (term): writeString() on current cursor pos */
    override fun writeChars(s: String) {
        /*currentJob = JOB_PRINTCHAR
        currentJobLen = s.length
        currentJobQueue = s*/
        for (i in 0..s.length - 1)
            printChar(s[i])
    }

    /** (TTY): Prints a series of chars and move cursor accordingly, then LF
     * (term): writeString() on current cursor pos */
    override fun printChars(s: String) {
        /*currentJob = JOB_PRINTCHAR
        currentJobLen = s.length + 1
        currentJobQueue = "$s\n"*/
        writeChars("$s\n")
    }

    override fun newLine() {
        lineBuffer = StringBuilder()
    }

    override fun scroll(amount: Int) {
        if (amount < 0) throw IllegalArgumentException("cannot scroll up")
        if (amount == 1) { newLine(); return }

        currentJob = JOB_LINEFEED
        currentJobLen = amount
    }

    override fun bell(pattern: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun wrap() {
        if (cursorX < 0) cursorX = 0
        else if (cursorX >= width) newLine()
    }

    var sb: StringBuilder = StringBuilder()
    private var inputOpen = false
    val DEBUG = true

    /**
     * Technically, this is different from Java's InputStream
     */
    override fun openInput() {
        lastStreamInput = null
        lastKeyPress = null
        inputOpen = true
        if (DEBUG) println("[TeletypeTerminal] openInput()")
    }

    override fun closeInputKey(keyFromUI: Int): Int {
        inputOpen = false
        lastKeyPress = keyFromUI

        if (DEBUG) println("[TeletypeTerminal] closeInputKey(), $keyFromUI")
        return keyFromUI
    }

    override var lastStreamInput: String? = null
    override var lastKeyPress: Int? = null

    override fun closeInputString(): String {
        inputOpen = false
        lastStreamInput = sb.toString()
        sb = StringBuilder()

        if (DEBUG) println("[TeletypeTerminal] closeInputString(), $lastStreamInput")
        return lastStreamInput!!
    }

    override fun keyPressed(key: Int, c: Char) {
        if (inputOpen) {
            if (c == SimpleTextTerminal.ASCII_CR)
                printChar(SimpleTextTerminal.ASCII_LF)
            else
                printChar(c)
            if (!SimpleTextTerminal.asciiControlInUse.contains(c)) sb.append(c)
            else if (c == SimpleTextTerminal.ASCII_DEL && sb.length > 0) sb.deleteCharAt(sb.length - 1)
        }
    }

    class TTYFont : Font {

        internal val fontSheet: SpriteSheet

        internal val W = 9
        internal val H = 12

        private val chars = arrayOf(
                '0','1','2','3','4','5','6','7',
                '8','9','[','#','@',':','>','?',
                ' ','A','B','C','D','E','F','G',
                'H','I','&','.',']','(','<','\\',
                '^','J','K','L','M','N','O','P', // ^: escape for capital letter
                'Q','R','-','¤','*',')',';','\'',
                '+','/','S','T','U','V','W','X',
                'Y','Z','_',',','%','=','"','!'
        )
        private val mappingTable = HashMap<Int, Int>()

        init {
            fontSheet = SpriteSheet("./assets/graphics/fonts/teletype_9x12.png", W, H)
            chars.forEachIndexed { i, c -> mappingTable[c.toInt()] = i }
        }

        override fun getHeight(str: String): Int = H

        override fun getWidth(str: String): Int {
            var ret = 0
            for (i in 0..str.length - 1)
                ret += W
            return ret
        }

        override fun getLineHeight(): Int = H

        override fun drawString(x: Float, y: Float, text: String) = drawString(x, y, text, Color.white)

        override fun drawString(x: Float, y: Float, text: String, col: Color) {
            var thisCol = col
            var textPosOffset = 0

            for (i in 0..text.length - 1) {
                val index = charToSpriteNum(text.toUpperCase().codePointAt(i))
                val ch = text[i]

                if (index != null) {
                    // main
                    fontSheet.getSubImage(index % 8, index / 8).draw(
                            x + textPosOffset, y, thisCol
                    )
                }
                textPosOffset += W
            }
        }

        override fun drawString(x: Float, y: Float, text: String, col: Color, startIndex: Int, endIndex: Int) {
            throw UnsupportedOperationException()
        }

        private fun charToSpriteNum(ch: Int): Int? = mappingTable[ch]
    }
}