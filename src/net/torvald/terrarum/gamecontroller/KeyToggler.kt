package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import net.torvald.util.sortedArrayListOf
import java.util.*

object KeyToggler {

    private val currentState = BitSet(256)
    private val isPressed = BitSet(256)
    private val isToggled = BitSet(256)

    /**
     * Keys that won't be updated when console is opened
     */
    private val gameKeys = sortedArrayListOf(
            Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9, Input.Keys.NUM_0, Input.Keys.MINUS, Input.Keys.EQUALS,
            Input.Keys.Q, Input.Keys.W, Input.Keys.E, Input.Keys.R, Input.Keys.T, Input.Keys.Y, Input.Keys.U, Input.Keys.I, Input.Keys.O, Input.Keys.P, Input.Keys.LEFT_BRACKET, Input.Keys.RIGHT_BRACKET,
            Input.Keys.A, Input.Keys.S, Input.Keys.D, Input.Keys.F, Input.Keys.G, Input.Keys.H, Input.Keys.J, Input.Keys.K, Input.Keys.L, Input.Keys.SEMICOLON, Input.Keys.APOSTROPHE,
            Input.Keys.Z, Input.Keys.X, Input.Keys.C, Input.Keys.V, Input.Keys.B, Input.Keys.N, Input.Keys.M, Input.Keys.COMMA, Input.Keys.PERIOD, Input.Keys.SLASH,
            Input.Keys.NUMPAD_0, Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4, Input.Keys.NUMPAD_5, Input.Keys.NUMPAD_6, Input.Keys.NUMPAD_7, Input.Keys.NUMPAD_8, Input.Keys.NUMPAD_9
    )


    fun isOn(key: Int): Boolean {
        return currentState[key]
    }

    /**
     * Put this into the each scene's update/render method.
     *
     * Set ```toggleGameKeys = true``` to make toggling work for keys like Q, W, E, ...; otherwise only F1-F12 keys will be toggled
     */
    fun update(toggleGameKeys: Boolean) {
        for (it in 0..255) {
            if (!toggleGameKeys && gameKeys.contains(it)) {
                continue
            }

            isPressed[it] = Gdx.input.isKeyPressed(it)

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
