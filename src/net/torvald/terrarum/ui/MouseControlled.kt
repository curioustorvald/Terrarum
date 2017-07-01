package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 16-03-06.
 */
interface MouseControlled {
    fun mouseMoved(screenX: Int, screenY: Int): Boolean
    fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean
    fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    fun scrolled(amount: Int): Boolean
}