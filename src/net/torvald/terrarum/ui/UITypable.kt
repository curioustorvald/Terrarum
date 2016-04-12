package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 16-03-06.
 */
interface UITypable {
    fun keyPressed(key: Int, c: Char)

    fun keyReleased(key: Int, c: Char)
}