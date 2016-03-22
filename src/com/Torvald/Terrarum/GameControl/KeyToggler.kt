package com.Torvald.Terrarum.GameControl

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input

object KeyToggler {

    private val currentState = BooleanArray(256)
    private val isPressed = BooleanArray(256)
    private val isToggled = BooleanArray(256)

    fun isOn(key: Int): Boolean {
        return currentState[key]
    }

    fun update(input: Input) {
        for (i in 0..255) {
            if (input.isKeyDown(i)) {
                isPressed[i] = true
            }
            else {
                isPressed[i] = false
            }
        }

        for (i in 0..255) {
            if (isPressed[i] && !currentState[i] && !isToggled[i]) {
                currentState[i] = true
                isToggled[i] = true
            }
            else if (isPressed[i] && currentState[i] && !isToggled[i]) {
                currentState[i] = false
                isToggled[i] = true
            }

            if (!isPressed[i] && isToggled[i]) {
                isToggled[i] = false
            }
        }
    }

    fun forceSet(key: Int, b: Boolean) {
        currentState[key] = b
        isToggled[key] = true
    }

}
