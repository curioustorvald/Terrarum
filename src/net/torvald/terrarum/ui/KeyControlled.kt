package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 16-03-06.
 */
interface KeyControlled {
    fun keyDown(keycode: Int): Boolean
    fun keyUp(keycode: Int): Boolean
    fun keyTyped(character: Char): Boolean
}