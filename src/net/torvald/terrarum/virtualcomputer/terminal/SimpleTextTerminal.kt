package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.aa.AAFrame
import net.torvald.aa.ColouredFastFont
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendMul
import net.torvald.terrarum.blendScreen
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import org.newdawn.slick.*
import java.nio.ByteBuffer

/**
 * Default text terminal, four text colours (black, grey, lgrey, white).
 *
 * Created by minjaesong on 16-09-07.
 */
open class SimpleTextTerminal(
        val phosphor: Color, override val width: Int, override val height: Int
) : Terminal {
    /**
     * Terminals must support AT LEAST 4 colours.
     * Color index 0 must be default background, index 3 must be default foreground
     */
    open protected val colours = arrayOf(
            Color(0x00, 0x00, 0x00), // black
            Color(0xff, 0xff, 0xff), // white
            Color(0x55, 0x55, 0x55), // dim grey
            Color(0xaa, 0xaa, 0xaa)  // light grey
    )                                // THESE ARE THE STANDARD

    override val coloursCount: Int
        get() = colours.size

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

    open protected val fontRef = "./assets/graphics/fonts/MDA.png"
    open protected val fontImg = Image(fontRef)
    open protected val fontW = fontImg.width / 16
    open protected val fontH = fontImg.height / 16
    open protected val font = ColouredFastFont(this, fontRef, fontW, fontH)

    override val displayW = fontW * width
    override val displayH = fontH * height


    private val TABSIZE = 4

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

        // beep AL-related
        if (beepSource != null && AL10.alGetSourcei(beepSource!!, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alDeleteSources(beepSource!!)
            AL10.alDeleteBuffers(beepBuffer!!)
            beepSource = null
            beepBuffer == null
        }
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

    open protected val colourScreen = Color(0x191919)

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
        g.color = getColor(foreDefault)
        if (cursorBlinkOn && cursorBlink)
            g.fillRect(
                    fontW * cursorX.toFloat(),
                    fontH * cursorY.toFloat(),
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

    override fun setCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    /** Emits a bufferChar. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    override fun emitChar(bufferChar: Int) {
        screenBuffer.drawBuffer(cursorX, cursorY, bufferChar.toChar())
    }

    /** Emits a char. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    override fun emitChar(c: Char) {
        screenBuffer.drawBuffer(cursorX, cursorY, c.toInt().and(0xFF).toChar(), colourKey)
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
        emitString(s)
        val absCursorPos = cursorX + cursorY * width + s.length
        setCursor(absCursorPos % width, absCursorPos / width)
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

    private val sampleRate = 22050
    private val maxDuration = 10000
    private val beepSamples = maxDuration.div(1000).times(sampleRate)
    private var beepSource: Int? = null
    private var beepBuffer: Int? = null

    override fun beep(freq: Float, duration: Int) {
        throw NotImplementedError("errenous OpenAL behaviour; *grunts*")

        val audioData = BufferUtils.createByteBuffer(duration.times(sampleRate).div(1000))

        var chop = false
        val realDuration = Math.min(maxDuration, duration)

        for (i in 0..realDuration - 1) {
            if (i.mod(freq) < 1.0) chop = !chop
            audioData.put(if (chop) 0xFF.toByte() else 0x00.toByte())
        }

        audioData.rewind()



        // Clear error stack.
        AL10.alGetError()

        beepBuffer = AL10.alGenBuffers()
        checkALError()

        try {
            AL10.alBufferData(beepBuffer!!, AL10.AL_FORMAT_MONO8, audioData, sampleRate)
            checkALError()

            beepSource = AL10.alGenSources()
            checkALError()

            try {
                AL10.alSourceQueueBuffers(beepSource!!, beepBuffer!!)
                checkALError()

                AL10.alSource3f(beepSource!!, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_REFERENCE_DISTANCE, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_MAX_DISTANCE, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_GAIN, 0.3f)
                checkALError()

                AL10.alSourcePlay(beepSource!!)
                checkALError()

            }
            catch (e: ALException) {
                AL10.alDeleteSources(beepSource!!)
            }
        }
        catch (e: ALException) {
            if (beepSource != null) AL10.alDeleteSources(beepSource!!)
        }


        /*def checkFinished = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING && {
            AL10.alDeleteSources(source)
            AL10.alDeleteBuffers(buffer)
            true
        }*/
    }

    // Custom implementation of Util.checkALError() that uses our custom exception.
    private fun checkALError() {
        val errorCode = AL10.alGetError()
        if (errorCode != AL10.AL_NO_ERROR) {
            throw ALException(errorCode)
        }
    }

    /** for "beep code" on modern BIOS. Pattern: - . */
    override fun bell(pattern: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var lastInputByte: Int = -1
    var sb: StringBuilder = StringBuilder()
    private var inputOpen = false
    /**
     * Technically, this is different from Java's InputStream
     */
    fun openInput() {
        inputOpen = true
        if (DEBUG) println("[SimpleTextTerminal] openInput()")
    }

    fun closeInput(): String {
        inputOpen = false
        val ret = sb.toString()
        sb = StringBuilder()

        if (DEBUG) println("[SimpleTextTerminal] closeInput(), $ret")
        return ret
    }

    override fun keyPressed(key: Int, c: Char) {
        lastInputByte = c.toInt()

        if (inputOpen) {
            if (c == ASCII_CR)
                printChar(ASCII_LF)
            else
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
        val IBM_GREEN = Color(74, 255, 0) // P39, 525 nm
        val WHITE = Color(228, 234, 255) // P4, 7 500 K
        val ELECTRIC_BLUE = Color(0, 239, 255) // imaginary, 486 nm
        val RED = Color(250, 0, 0) // <= 645 nm

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
                ASCII_DC4
        )
    }

    private val DEBUG = true
}

class ALException(errorCode: Int) : Exception("ALerror: $errorCode") {

}
