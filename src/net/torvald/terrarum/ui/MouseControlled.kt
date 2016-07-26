package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 16-03-06.
 */
interface MouseControlled {
    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int)

    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int)

    fun mousePressed(button: Int, x: Int, y: Int)

    fun mouseReleased(button: Int, x: Int, y: Int)

    fun mouseWheelMoved(change: Int)
}