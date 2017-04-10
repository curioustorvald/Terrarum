package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 16-03-06.
 */
interface KeyControlled {
    fun keyPressed(key: Int, c: Char)
    fun keyReleased(key: Int, c: Char)
    fun controllerButtonPressed(controller: Int, button: Int)
    fun controllerButtonReleased(controller: Int, button: Int)
}