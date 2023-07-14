package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent


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
 * PROTIP: if [clickOnceListener] does not seem to work, make sure your parent UI is handling touchDown() and touchUp() events!
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
    protected val itemRelativeMouseX: Int
        get() = (Terrarum.mouseScreenX - parentUI.posX - this.posX)
    /** Position of mouse relative to this item */
    protected val itemRelativeMouseY: Int
        get() = (Terrarum.mouseScreenY - parentUI.posY - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = itemRelativeMouseX in 0 until width && itemRelativeMouseY in 0 until height
    /** If mouse is hovering over it and mouse is down */
    val mousePushed: Boolean
        get() = mouseUp && Terrarum.mouseDown
    val mouseDown: Boolean
        get() = Terrarum.mouseDown


    /** to be used by customised mouse handling */
    protected var mouseLatched = false

    /** UI to call (show up) while mouse is up */
    open var mouseOverCall: UICanvas? = null


    // kind of listener implementation
    /** Fired once for every update
     * Parameter: delta */
    open var updateListener: ((Float) -> Unit) = {}
    /** Parameter: keycode */
    open var keyDownListener: ((Int) -> Unit) = {}
    /** Parameter: keycode */
    open var keyUpListener: ((Int) -> Unit) = {}
    open var keyTypedListener: ((Char) -> Unit) = {}
    open var touchDraggedListener: ((Int, Int, Int) -> Unit) = { _,_,_ -> }
    /** Parameters: screenX, screenY, pointer, button */
    @Deprecated("Not really deprecated but you MOST DEFINITELY want to use clickOnceListener(mouseX, mouseY) instead.")
    open var touchDownListener: ((Int, Int, Int, Int) -> Unit) = { _,_,_,_ -> }
    open var touchUpListener: ((Int, Int, Int, Int) -> Unit) = { _,_,_,_ -> }
    /** Parameters: amountX, amountY */
    open var scrolledListener: ((Float, Float) -> Unit) = { _,_ -> }
    /** Parameters: relative mouseX, relative mouseY
     * ClickOnce listeners are only fired when clicked with primary mouse button
     * PROTIP: if clickOnceListener does not seem to work, make sure your parent UI is handling touchDown() and touchUp() events!
     */
    open var clickOnceListener: ((Int, Int) -> Unit) = { _,_ -> }
    open var clickOnceListenerFired = false
    /** Parameters: relative mouseX, mouseY */
    open var mouseUpListener: ((Int, Int) -> Unit) = { _,_, -> }

    /** Since gamepads can't just choose which UIItem to control, this variable is used to allow processing of
     * gamepad button events for one or more UIItems in one or more UICanvases. */
    open var controllerInFocus = false

    /**
     * Whether the button is "available" or not to the player
     */
    open var isEnabled = true

    /**
     * Whether the button should receive updates
     */
    open var isActive = true


    open fun show() {}
    open fun hide() {}


    open fun update(delta: Float) {
        if (parentUI.isVisible) {
            if (isActive) {
                updateListener.invoke(delta)

                mouseOverCall?.update(delta)

                if (mouseUp) {
                    if (mouseOverCall?.isVisible == true) {
                        mouseOverCall?.setAsOpen()
                    }

                    mouseOverCall?.updateUI(delta)
                    mouseUpListener.invoke(itemRelativeMouseX, itemRelativeMouseY)
                }
                else {
                    if (mouseOverCall?.isVisible == true) {
                        mouseOverCall?.setAsClose()
                    }
                }

            }
        }
    }

    /**
     * In this time, you do write like: ```draw(posX + 4, posY + 32)```, unlike UICanvas, because posX/posY comes from the parent UI.
     */
    open fun render(batch: SpriteBatch, camera: Camera) {
        if (parentUI.isVisible) {
//            if (isActive) {
                mouseOverCall?.render(batch, camera)

                if (mouseUp) {
                    mouseOverCall?.renderUI(batch, camera)
                }
//            }
        }
    }

    // keyboard controlled
    open fun keyDown(keycode: Int): Boolean {
        if (parentUI.isVisible && isEnabled) {
            keyDownListener.invoke(keycode)
            return true
        }

        return false
    }
    open fun keyUp(keycode: Int): Boolean {
        if (parentUI.isVisible && isEnabled) {
            keyUpListener.invoke(keycode)
            return true
        }

        return false
    }
    open fun keyTyped(character: Char): Boolean  {
        if (parentUI.isVisible && isEnabled) {
            keyTypedListener.invoke(character)
            return true
        }

        return false
    }

    // mouse controlled
    open fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (parentUI.isVisible && isEnabled) {
            touchDraggedListener.invoke(itemRelativeMouseX, itemRelativeMouseY, pointer)
            return true
        }

        return false
    }
    open fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        var actionDone = false

        if (parentUI.isVisible && isEnabled) {
            if (mouseUp) {
                touchDownListener.invoke(itemRelativeMouseX, itemRelativeMouseY, pointer, button)
                actionDone = true
            }

            if (!clickOnceListenerFired && mouseUp && button == App.getConfigInt("config_mouseprimary")) {
                clickOnceListener.invoke(itemRelativeMouseX, itemRelativeMouseY)
                actionDone = true
            }
        }

        return actionDone
    }
    open fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        clickOnceListenerFired = false

        if (parentUI.isVisible && mouseUp) {
            touchUpListener.invoke(itemRelativeMouseX, itemRelativeMouseY, pointer, button)
            return true
        }

        return false
    }
    open fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (parentUI.isVisible && mouseUp && isEnabled) {
            scrolledListener.invoke(amountX, amountY)
            return true
        }

        return false
    }
    open fun inputStrobed(e: TerrarumKeyboardEvent) {

    }

    abstract override fun dispose()

}
