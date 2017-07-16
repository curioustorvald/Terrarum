package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum


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
        get() = (Terrarum.mouseScreenX - (parentUI.handler?.posX ?: 0) - this.posX)
    protected val relativeMouseY: Int
        get() = (Terrarum.mouseScreenY - (parentUI.handler?.posY ?: 0) - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = relativeMouseX in 0..width - 1 && relativeMouseY in 0..height - 1
    /** If mouse is hovering over it and mouse is down */
    open val mousePushed: Boolean
        get() = mouseUp && Gdx.input.isButtonPressed(Terrarum.getConfigInt("mouseprimary")!!)



    // kind of listener implementation
    var updateAction: ((Float) -> Unit)? = null
    var keyDownAction: ((Int) -> Unit)? = null
    var keyUpAction: ((Int) -> Unit)? = null
    var mouseMovedAction: ((Int, Int) -> Unit)? = null
    var touchDraggedAction: ((Int, Int, Int) -> Unit)? = null
    var touchDownAction: ((Int, Int, Int, Int) -> Unit)? = null
    var touchUpAction: ((Int, Int, Int, Int) -> Unit)? = null
    var scrolledAction: ((Int) -> Unit)? = null

    var clickOnceAction: ((Int, Int, Int) -> Unit)? = null
    var clickOnceActionEngaged = false



    open fun update(delta: Float) {
        if (updateAction != null) {
            updateAction!!.invoke(delta)
        }
    }
    abstract fun render(batch: SpriteBatch)

    // keyboard controlled
    open fun keyDown(keycode: Int): Boolean {
        if (keyDownAction != null) {
            keyDownAction!!.invoke(keycode)
            return true
        }

        return false
    }
    open fun keyUp(keycode: Int): Boolean {
        if (keyUpAction != null) {
            keyUpAction!!.invoke(keycode)
            return true
        }

        return false
    }

    // mouse controlled
    open fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (mouseMovedAction != null) {
            mouseMovedAction!!.invoke(relativeMouseX, relativeMouseY)
            return true
        }

        return false
    }
    open fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (touchDraggedAction != null) {
            touchDraggedAction!!.invoke(relativeMouseX, relativeMouseY, pointer)
            return true
        }

        return false
    }
    open fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        var actionDone = false

        if (touchDownAction != null) {
            touchDownAction!!.invoke(relativeMouseX, relativeMouseY, pointer, button)
            actionDone = true
        }

        if (!clickOnceActionEngaged && mouseUp) {
            clickOnceAction!!.invoke(relativeMouseX, relativeMouseY, button)
            actionDone = true
        }

        return actionDone
    }
    open fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        clickOnceActionEngaged = false

        if (touchUpAction != null) {
            touchUpAction!!.invoke(relativeMouseX, relativeMouseY, pointer, button)
            return true
        }

        return false
    }
    open fun scrolled(amount: Int): Boolean {
        if (scrolledAction != null) {
            scrolledAction!!.invoke(amount)
            return true
        }

        return false
    }

    abstract fun dispose()

}
