package net.torvald.terrarum.virtualcomputer.terminal

import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * A terminal
 *
 * Framebuffer: USE net.torvald.aa.AAFrame
 *
 * Background color is fixed; text color is variable
 *
 * Created by minjaesong on 16-09-07.
 */
interface Terminal : Teletype {
    override val width: Int
    val height: Int
    override val coloursCount: Int
    override var cursorX: Int
    var cursorY: Int
    var cursorBlink: Boolean
    var backColour: Int
    var foreColour: Int

    var lastInputByte: Int

    // to be used in UI
    override val displayW: Int
    val displayH: Int

    fun getColor(index: Int): Color
    override fun update(gc: GameContainer, delta: Int)
    override fun render(gc: GameContainer, g: Graphics)
    override fun keyPressed(key: Int, c: Char)

    // API calls
    fun setCursor(x: Int, y: Int)
    /** Emits a bufferChar. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    fun emitChar(bufferChar: Int, x: Int = cursorX, y: Int = cursorY)
    /** Emits a char. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    fun emitChar(c: Char, x: Int = cursorX, y: Int = cursorY)
    /** Prints a char and move cursor accordingly. */
    override fun printChar(c: Char)
    /** Emits a string, does not affected by control sequences. Does not move cursor */
    fun emitString(s: String, x: Int = cursorX, y: Int = cursorY)
    /** Emits a string and move cursor accordingly, then do LF */
    fun printString(s: String, x: Int = cursorX, y: Int = cursorY)
    /** Emits a string and move cursor accordingly. */
    fun writeString(s: String, x: Int = cursorX, y: Int = cursorY)
    fun clear()
    fun clearLine()
    override fun newLine()
    override fun scroll(amount: Int)
    fun setColour(back: Int, fore: Int)
    fun resetColour()
    /**
     * @param duration: milliseconds
     * @param freg: Frequency (float)
     */
    fun emitTone(duration: Int, freq: Float)

    override fun bell(pattern: String)
    /** Requires keyPressed() event to be processed.
     *
     *  null indicates the input stream is waiting for an input
     *
     *  implementation:
     *
     *  private var lastInputByte: Int? = null
     *  override fun keyPressed(key: Int, c: Char) {
            lastInputByte = c.toInt()
            lastInputByte = null
        }
     */
    fun getKeyPress(): Int?
}