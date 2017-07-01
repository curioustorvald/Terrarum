package net.torvald.terrarum.gameactors


/**
 * Actors that has movement controlled by Keyboard or AI
 *
 * Created by minjaesong on 15-12-31.
 */
interface Controllable {

    fun processInput(delta: Float)
    fun keyDown(keycode: Int): Boolean

}