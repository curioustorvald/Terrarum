package net.torvald.terrarum.ui

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 15-12-31.
 */
interface UIItem {

    // X/Y Position relative to the containing canvas
    var posX: Int
    var posY: Int

    fun update(gc: GameContainer, delta: Int)
    fun render(gc: GameContainer, g: Graphics)

    // keyboard controlled
    fun keyPressed(key: Int, c: Char)
    fun keyReleased(key: Int, c: Char)

    // mouse controlled
    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int)
    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int)
    fun mousePressed(button: Int, x: Int, y: Int)
    fun mouseReleased(button: Int, x: Int, y: Int)
    fun mouseWheelMoved(change: Int)

    // gamepad controlled
    fun controllerButtonPressed(controller: Int, button: Int)
    fun controllerButtonReleased(controller: Int, button: Int)
}
