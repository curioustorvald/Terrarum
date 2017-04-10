package net.torvald.terrarum.ui

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gamecontroller.mouseScreenX
import net.torvald.terrarum.gamecontroller.mouseScreenY
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 15-12-31.
 */
abstract class UIItem(var parentUI: UICanvas) { // do not replace parentUI to UIHandler!

    // X/Y Position relative to the containing canvas
    abstract var posX: Int
    abstract var posY: Int
    abstract val width: Int
    abstract val height: Int

    protected val relativeMouseX: Int
        get() = (Terrarum.appgc.mouseScreenX - (parentUI.handler?.posX ?: 0) - this.posX)
    protected val relativeMouseY: Int
        get() = (Terrarum.appgc.mouseScreenY - (parentUI.handler?.posY ?: 0) - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = relativeMouseX in 0..width - 1 && relativeMouseY in 0..height - 1
    /** If mouse is hovering over it and mouse is down */
    open val mousePushed: Boolean
        get() = mouseUp && Terrarum.appgc.input.isMouseButtonDown(Terrarum.getConfigInt("mouseprimary")!!)

    abstract fun update(gc: GameContainer, delta: Int)
    abstract fun render(gc: GameContainer, g: Graphics)

    // keyboard controlled
    abstract fun keyPressed(key: Int, c: Char)
    abstract fun keyReleased(key: Int, c: Char)

    // mouse controlled
    abstract fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int)
    abstract fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int)
    abstract fun mousePressed(button: Int, x: Int, y: Int)
    abstract fun mouseReleased(button: Int, x: Int, y: Int)
    abstract fun mouseWheelMoved(change: Int)

    // gamepad controlled
    abstract fun controllerButtonPressed(controller: Int, button: Int)
    abstract fun controllerButtonReleased(controller: Int, button: Int)
}
