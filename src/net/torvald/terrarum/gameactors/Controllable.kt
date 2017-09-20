package net.torvald.terrarum.gameactors


/**
 * Actors that has movement controlled by Keyboard or AI
 *
 * Created by minjaesong on 2015-12-31.
 */
interface Controllable {
    fun keyDown(keycode: Int): Boolean

}