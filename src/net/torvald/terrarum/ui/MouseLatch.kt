package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.App
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by minjaesong on 2024-01-10.
 */
class MouseLatch(val button: List<Int> = listOf(App.getConfigInt("config_mouseprimary"))) {

    private val status = AtomicBoolean()

    /**
     * Performs the `action` when the mouse-latch is unlatched and any of the specified buttons are down. After the `action` has run, the unlatch check will be done immediately.
     *
     * @param action what to do when the mouse is clicked. The latch will be latched right before the action runs.
     * Return `false` to unlatch the mouse-latch. Returning null WILL latch it.
     */
    fun latchSelectively(action: () -> Boolean?) {
        if (isNotLatched() && button.any { Gdx.input.isButtonPressed(it) }) {
            status.set(true)
            status.set(action() ?: true)
        }

        if (isLatched() && button.none { Gdx.input.isButtonPressed(it) }) {
            status.set(false)
        }
    }

    /**
     * Performs the `action` when the mouse-latch is unlatched and any of the specified buttons are down. After the `action` has run, the unlatch check will be done immediately.
     *
     * @param action what to do when the mouse is clicked. The mouse-latch will be latched right before the action runs
     */
    fun latch(action: () -> Unit) {
        if (isNotLatched() && button.any { Gdx.input.isButtonPressed(it) }) {
            status.set(true)
            action()
        }

        if (isLatched() && button.none { Gdx.input.isButtonPressed(it) }) {
            status.set(false)
        }
    }

    fun unlatch() {
        if (button.none { Gdx.input.isButtonPressed(it) }) {
            status.set(false)
        }
    }

    fun isLatched() = status.get()
    fun isNotLatched() = !status.get()

}