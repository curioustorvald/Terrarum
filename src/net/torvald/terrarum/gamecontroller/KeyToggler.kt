package net.torvald.terrarum.gamecontroller

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import java.util.*

object KeyToggler {

    private val currentState = BitSet(256)
    private val isPressed = BitSet(256)
    private val isToggled = BitSet(256)

    fun isOn(key: Int): Boolean {
        return currentState[key]
    }

    fun update(input: Input) {
        (0..255).forEach {
            isPressed[it] = input.isKeyDown(it)

            if (isPressed[it] && !currentState[it] && !isToggled[it]) {
                currentState[it] = true
                isToggled[it] = true
            }
            else if (isPressed[it] && currentState[it] && !isToggled[it]) {
                currentState[it] = false
                isToggled[it] = true
            }

            if (!isPressed[it] && isToggled[it]) {
                isToggled[it] = false
            }
        }
    }

    fun forceSet(key: Int, b: Boolean) {
        currentState[key] = b
        isToggled[key] = true
    }

}
