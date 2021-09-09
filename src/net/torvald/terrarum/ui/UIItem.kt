package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum


/**
 * ## Attaching Input Listeners
 *
 * UIItem provides following listeners:
 *
 * - updateListener
 * - keyDownListener
 * - keyUpListener
 * - touchDraggedLister
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
 * Kotlin:
 * <<identifier>>.clickOnceListener = { mouseX, mouseY, button ->
 *     println("Bo-ing!")
 * }
 *
 * Java:
 * <<identifier>>.setClickOnceListener((mouseX, mouseY, button) -> {
 *     System.out.println("Bo-ing!");
 *     return null;
 * });
 * ```
 *
 * This listener will print out 'Bo-ing!' whenever it's clicked.
 *
 * As mentioned in [UICanvas], UIItems must be added to the Canvas to make listeners work without implementing
 * everything by yourself.
 *
 * @param initialX initial position of the item. Useful for making transition that requires the item to be moved
 * @param initialY initial position of the item. Useful for making transition that requires the item to be moved
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class UIItem(var parentUI: UICanvas, val initialX: Int, val initialY: Int): Disposable { // do not replace parentUI to UIHandler!

    // X/Y Position relative to the containing canvas
    var posX: Int = initialX
    var posY: Int = initialY
    abstract val width: Int
    abstract val height: Int

    /** This variable is NOT updated on its own.
     * ```
     * val posXDelta = posX - oldPosX
     * itemGrid.forEach { it.posX += posXDelta }
     * ...
     * oldPosX = posX
     * ```
     */
    protected var oldPosX: Int = initialX
    /** This variable is NOT updated on its own.
     * ```
     * val posYDelta = posY - oldPosY
     * itemGrid.forEach { it.posY += posYDelta }
     * ...
     * oldPosY = posY
     * ```
     */
    protected var oldPosY: Int = initialY

    /** Position of mouse relative to this item */
    protected val relativeMouseX: Int
        get() = (Terrarum.mouseScreenX - (parentUI.posX) - this.posX)
    /** Position of mouse relative to this item */
    protected val relativeMouseY: Int
        get() = (Terrarum.mouseScreenY - (parentUI.posY) - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = relativeMouseX in 0..width - 1 && relativeMouseY in 0..height - 1
    /** If mouse is hovering over it and mouse is down */
    open val mousePushed: Boolean
        get() = mouseUp && Gdx.input.isButtonPressed(App.getConfigInt("config_mouseprimary"))


    /** UI to call (show up) while mouse is up */
    open val mouseOverCall: UICanvas? = null


    // kind of listener implementation
    /** Fired once for every update
     * Parametre: delta */
    open var updateListener: ((Float) -> Unit)? = null
    /** Parametre: keycode */
    open var keyDownListener: ((Int) -> Unit)? = null
    /** Parametre: keycode */
    open var keyUpListener: ((Int) -> Unit)? = null
    open var touchDraggedListener: ((Int, Int, Int) -> Unit)? = null
    /** Parameters: screenX, screenY, pointer, button */
    open var touchDownListener: ((Int, Int, Int, Int) -> Unit)? = null
    open var touchUpListener: ((Int, Int, Int, Int) -> Unit)? = null
    /** Parameters: amountX, amountY */
    open var scrolledListener: ((Float, Float) -> Unit)? = null
    /** Parameters: relative mouseX, relative mouseY, button */
    open var clickOnceListener: ((Int, Int, Int) -> Unit)? = null
    open var clickOnceListenerFired = false


    /** Since gamepads can't just choose which UIItem to control, this variable is used to allow processing of
     * gamepad button events for one or more UIItems in one or more UICanvases. */
    open var controllerInFocus = false


    open fun update(delta: Float) {
        if (parentUI.isVisible) {
            if (updateListener != null) {
                updateListener!!.invoke(delta)
            }


            mouseOverCall?.update(delta)

            if (mouseUp) {
                if (mouseOverCall?.isVisible ?: false) {
                    mouseOverCall?.setAsOpen()
                }

                mouseOverCall?.updateUI(delta)
            }
            else {
                if (mouseOverCall?.isVisible ?: false) {
                    mouseOverCall?.setAsClose()
                }
            }
        }
    }

    /**
     * In this time, you do write like: ```draw(posX + 4, posY + 32)```, unlike UICanvas, because posX/posY comes from the parent UI.
     */
    open fun render(batch: SpriteBatch, camera: Camera) {
        if (parentUI.isVisible) {
            mouseOverCall?.render(batch, camera)

            if (mouseUp) {
                mouseOverCall?.renderUI(batch, camera)
            }
        }
    }

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
    open fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (parentUI.isVisible && scrolledListener != null) {
            scrolledListener!!.invoke(amountX, amountY)
            return true
        }

        return false
    }

    abstract override fun dispose()

}
