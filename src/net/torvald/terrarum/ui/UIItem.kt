package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum


/**
 * ## Attaching Input Listeners
 *
 * UIItem provides following listeners:
 *
 * - updateListener
 * - keyDownListener
 * - keyUpListener
 * - mouseMovedListene
 * - touchDraggedListe
 * - touchDownListener
 * - touchUpListener
 * - scrolledListener
 * - clickOnceListener
 *
 * Each listeners are implemented using _functions_, instead of traditional listener _classes_.
 * What you should do is just override one or more of these variables which has 'function' as their type.
 * For example:
 *
 * ```
 * <<some_name>>.clickOnceListener = { mouseX, mouseY, button ->
 *     println("Bo-ing!")
 * }
 * ```
 *
 * This listener will print out 'Bo-ing!' whenever it's clicked.
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class UIItem(var parentUI: UICanvas) { // do not replace parentUI to UIHandler!

    // X/Y Position relative to the containing canvas
    abstract var posX: Int
    abstract var posY: Int
    abstract val width: Int
    abstract val height: Int

    protected val relativeMouseX: Int
        get() = (Terrarum.mouseScreenX - (parentUI.posX) - this.posX)
    protected val relativeMouseY: Int
        get() = (Terrarum.mouseScreenY - (parentUI.posY) - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = relativeMouseX in 0..width - 1 && relativeMouseY in 0..height - 1
    /** If mouse is hovering over it and mouse is down */
    open val mousePushed: Boolean
        get() = mouseUp && Gdx.input.isButtonPressed(Terrarum.getConfigInt("mouseprimary")!!)



    // kind of listener implementation
    var updateListener: ((Float) -> Unit)? = null
    var keyDownListener: ((Int) -> Unit)? = null
    var keyUpListener: ((Int) -> Unit)? = null
    var mouseMovedListener: ((Int, Int) -> Unit)? = null
    var touchDraggedListener: ((Int, Int, Int) -> Unit)? = null
    var touchDownListener: ((Int, Int, Int, Int) -> Unit)? = null
    var touchUpListener: ((Int, Int, Int, Int) -> Unit)? = null
    var scrolledListener: ((Int) -> Unit)? = null

    var clickOnceListener: ((Int, Int, Int) -> Unit)? = null
    var clickOnceListenerFired = false



    open fun update(delta: Float) {
        if (parentUI.isVisible) {
            if (updateListener != null) {
                updateListener!!.invoke(delta)
            }
        }
    }
    abstract fun render(batch: SpriteBatch)

    // keyboard controlled
    open fun keyDown(keycode: Int): Boolean {
        if (parentUI.isVisible && keyDownListener != null) {
            keyDownListener!!.invoke(keycode)
            return true
        }

        return false
    }
    open fun keyUp(keycode: Int): Boolean {
        if (parentUI.isVisible && keyUpListener != null) {
            keyUpListener!!.invoke(keycode)
            return true
        }

        return false
    }

    // mouse controlled
    open fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (parentUI.isVisible && mouseMovedListener != null) {
            mouseMovedListener!!.invoke(relativeMouseX, relativeMouseY)
            return true
        }

        return false
    }
    open fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (parentUI.isVisible && touchDraggedListener != null) {
            touchDraggedListener!!.invoke(relativeMouseX, relativeMouseY, pointer)
            return true
        }

        return false
    }
    open fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        var actionDone = false

        if (parentUI.isVisible) {
            if (touchDownListener != null) {
                touchDownListener!!.invoke(relativeMouseX, relativeMouseY, pointer, button)
                actionDone = true
            }

            if (clickOnceListener != null && !clickOnceListenerFired && mouseUp) {
                clickOnceListener!!.invoke(relativeMouseX, relativeMouseY, button)
                actionDone = true
            }
        }

        return actionDone
    }
    open fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        clickOnceListenerFired = false

        if (parentUI.isVisible && touchUpListener != null) {
            touchUpListener!!.invoke(relativeMouseX, relativeMouseY, pointer, button)
            return true
        }

        return false
    }
    open fun scrolled(amount: Int): Boolean {
        if (parentUI.isVisible && scrolledListener != null) {
            scrolledListener!!.invoke(amount)
            return true
        }

        return false
    }

    abstract fun dispose()

}
