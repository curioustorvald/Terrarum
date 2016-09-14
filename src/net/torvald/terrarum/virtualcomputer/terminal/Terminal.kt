package net.torvald.terrarum.virtualcomputer.terminal

import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * A tty (terminal)
 *
 * Framebuffer : use net.torvald.aa.AAFrame
 *
 * Background color is fixed; text color is variable
 *
 * Created by minjaesong on 16-09-07.
 */
interface Terminal {
    val width: Int
    val height: Int
    val coloursCount: Int
    var cursorX: Int
    var cursorY: Int
    var cursorBlink: Boolean
    var backColour: Int
    var foreColour: Int

    var lastInputByte: Int

    // to be used in UI
    val displayW: Int
    val displayH: Int

    fun getColor(index: Int): Color
    fun update(gc: GameContainer, delta: Int)
    fun render(gc: GameContainer, g: Graphics)
    fun keyPressed(key: Int, c: Char)

    // API calls
    fun setCursor(x: Int, y: Int)
    /** Emits a bufferChar. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    fun emitChar(bufferChar: Int)
    /** Emits a char. Does not move cursor
     *  It is also not affected by the control sequences; just print them out as symbol */
    fun emitChar(c: Char)
    /** Prints a char and move cursor accordingly. */
    fun printChar(c: Char)
    /** Emits a string. Does not move cursor */
    fun emitString(s: String)
    /** Emits a string and move cursor accordingly. */
    fun printString(s: String, x: Int = cursorX, y: Int = cursorY)
    fun clear()
    fun clearLine()
    fun scroll(amount: Int = 1)
    fun setColour(back: Int, fore: Int)
    fun resetColour()
    /**
     * @param freg: Frequency (float)
     * @param duration: milliseconds
     */
    fun beep(freq: Float = 1000f, duration: Int = 200)
    /** for "beep code" on modern BIOS. Pattern: - . */
    fun beep(pattern: String)
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