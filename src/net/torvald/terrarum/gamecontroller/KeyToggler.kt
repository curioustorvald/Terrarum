package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.TerrarumGDX
import java.util.*

object KeyToggler {

    private val currentState = BitSet(256)
    private val isPressed = BitSet(256)
    private val isToggled = BitSet(256)

    /**
     * Keys that won't be updated when console is opened
     */
    private val gameKeys = (16..27) + (30..40) + (43..53)


    fun isOn(key: Int): Boolean {
        return currentState[key]
    }

    fun update(gameMode: Boolean = true) {
        for (it in 0..255) {
            if (gameMode && it in gameKeys &&
                (TerrarumGDX.ingame!!.consoleHandler.isOpening || TerrarumGDX.ingame!!.consoleHandler.isOpened)) {
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
