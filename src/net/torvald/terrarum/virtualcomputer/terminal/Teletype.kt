package net.torvald.terrarum.virtualcomputer.terminal

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-09-14.
 */
interface Teletype {
    val width: Int
    val displayW: Int

    var cursorX: Int

    /**
     * 0: Teletype
     * 4: Non-colour terminal (Note that '2' is invalid!)
     * >4: Colour terminal
     */
    val coloursCount: Int

    fun update(gc: GameContainer, delta: Int)
    fun render(gc: GameContainer, g: Graphics)
    fun keyPressed(key: Int, c: Char)

    /** Prints a char and move cursor accordingly */
    fun printChar(c: Char)
    /** (TTY): Prints a series of chars and move cursor accordingly, then LF
     * (term): printString() on current cursor pos */
    fun printChars(s: String)
    /** (TTY): Prints a series of chars and move cursor accordingly
     * (term): writeString() on current cursor pos */
    fun writeChars(s: String)
    fun newLine()
    fun scroll(amount: Int = 1)

    fun openInput()
    fun closeInputKey(keyFromUI: Int): Int
    fun closeInputString(): String

    fun bell(pattern: String = ".")

    var lastStreamInput: String?
    var lastKeyPress: Int?
}