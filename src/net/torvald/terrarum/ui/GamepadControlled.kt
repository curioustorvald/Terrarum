package net.torvald.terrarum.ui

/**
 * Button pressing only. If you want stick-controls, use processInput()
 *
 * Created by minjaesong on 16-07-21.
 */
interface GamepadControlled {
    fun controllerButtonPressed(controller: Int, button: Int)
    fun controllerButtonReleased(controller: Int, button: Int)
}