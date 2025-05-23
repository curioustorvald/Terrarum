package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TooltipListener
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import kotlin.math.max
import kotlin.math.roundToInt


/**
 * ## UI Items
 *
 * UI can contain one or more UI elements (called UIItem). Each UIItem can have one or more events programmed to it.
 * Events have their own listener are governed by their GDX event handlers (e.g. touchDragged).
 * These GDX handlers are what makes the our own handler to work.
 *
 * UIItems have following event handlers: updateLister, keyDownListener, touchDraggedListener, touchDownListener, touchUpListener, scrolledListener, and clickOnceListener.
 * (perhaps clickOnceListener is the one most useful)
 *
 * To make them work without any hassle on your part,
 * all the UIItems must be added to this UICanvas's ```uiItems``` list.
 *
 * See also: [net.torvald.terrarum.ui.UIItem]
 *
 * ## Sub UIs
 *
 * Sub UIs are UICanvases that is child of this UICanvas. They are also managed internally to lessen your burden.
 * Just add all the Sub UIs using ```addSubUI()``` method.
 *
 * ## Position variables
 *
 * PosX/Y and relativeMouseX/Y are explained in ```work_files/terrarum_ui_elements_coord_explained.png```
 *
 * ## Show/Hide/Opening/Closing Timeline
 *
 * The functions `show`, `doOpening`, `doClosing`, `hide`, `endOpening`, `endClosing` are called sequentially in this order:
 *
 * ```
 * (open has triggered)
 * show()
 * doOpening()
 * doOpening()
 * ...
 * endOpening()
 * ...
 * (close has triggered)
 * doClosing()
 * doClosing()
 * ...
 * endClosing()
 * hide()
 * ```
 * NOTE: show/hide is a UICanvas function, while do/endSomething() is UIHandler function.
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class UICanvas(
        toggleKeyLiteral: String? = null, toggleButtonLiteral: String? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
): TooltipListener(), Disposable {

    internal var openingClickLatched = false

    val justOpened: Boolean
        get() = handler.justOpened

    abstract var width: Int
    abstract var height: Int

    inline var posX: Int
        get() = handler.posX
        set(value) { handler.posX = value }
    inline var posY: Int
        get() = handler.posY
        set(value) { handler.posY = value }

    inline var initialX: Int
        get() = handler.initialX
        set(value) { handler.initialX = value }
    inline var initialY: Int
        get() = handler.initialY
        set(value) { handler.initialY = value }

    /**
     * Usage: (in StateInGame:) uiHandlerField.ui.handler = uiHandlerField
     */
    val handler = UIHandler(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant)

    /**
     * In milliseconds
     *
     * Timer itself is implemented in the ui.handler.
     */
    open var openCloseTime: Second = OPENCLOSE_GENERIC


    protected val uiItems = ArrayList<UIItemisable>()


    val relativeMouseX: Int
        get() = Terrarum.mouseScreenX - handler.posX
    val relativeMouseY: Int
        get() = Terrarum.mouseScreenY - handler.posY

    /** If mouse is hovering over it regardless of its visibility */
    open val mouseUp: Boolean
        get() = _mouseUpThis || handler.mouseUp
    /** If mouse is hovering over it and mouse is down */
    val mousePushed: Boolean
        get() = mouseUp && Terrarum.mouseDown

    private val _mouseUpThis: Boolean
        get() = relativeMouseX in 0 until width && relativeMouseY in 0 until height

    /** Called by the screen */
    fun update(delta: Float) {
        handler.update(this, delta)
    }
    /** Called by the screen */
    fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera, parentOpacity: Float = 1f) {
        handler.render(this, frameDelta, batch, camera, parentOpacity)
    }


    fun addSubUI(ui: UICanvas) {
        handler.addSubUI(ui)
    }


    /** A function that is run ONCE when the UI is requested to be opened; will work identical to [endOpening] if [openCloseTime] is zero */
    open fun show() {
        clearTooltip()
        openingClickLatched = true
        uiItems.forEach { it.show() }
        handler.subUIs.forEach { it.show() }
    }
    /** A function that is run ONCE when the UI is requested to be closed; will work identical to [endClosing] if [openCloseTime] is zero */
    open fun hide() {
        uiItems.forEach { it.hide() }
        handler.subUIs.forEach { it.hide() }
        openingClickLatched = true // just in case `justOpened` detection fails
    }


    /** **DO NOT CALL THIS FUNCTION FOR THE ACTUAL UPDATING OF THE UI — USE update() INSTEAD**
     *
     * Override this for the actual update. Note that you must update uiItems by yourself. */
    abstract fun updateImpl(delta: Float)
    /** **DO NOT CALL THIS FUNCTION FOR THE ACTUAL RENDERING OF THE UI — USE render() INSTEAD**
     *
     * Override this for the actual render. Note that you must render uiItems by yourself.
     *
     * Under normal circumstances, draws are automatically translated as per the handler's X/Y position.
     * This means, don't write like: ```draw(posX + 4, posY + 32)```, do instead: ```draw(4, 32)``` unless you have a good reason to do so.
     *
     * The transparency of the handler is independent of the draw, you must set the drawing color yourself
     * (use handler.opacity or handler.opacityColour)
     */
    abstract fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera)

    /**
     * Do not modify ui.handler.openCloseCounter here.
     *
     * When you override this function, you usually append either of following functions:
     * - `INGAME.pause()` to literally pause the game and disable the player control
     * - `INGAME.disablePlayerControl()` to not pause the game but still disable the player control
     */
    open fun doOpening(delta: Float) {
        handler.opacity = handler.openCloseCounter / openCloseTime
        clearTooltip()
    }

    /**
     * Do not modify ui.handler.openCloseCounter here.
     *
     * When you override this function, you usually append either of following functions:
     * - `INGAME.resume()` if your `doOpening()` paused the game
     * - `INGAME.resumePlayerControl()` if `doOpening()` disabled the player control
     */
    open fun doClosing(delta: Float) {
        handler.opacity = (openCloseTime - handler.openCloseCounter) / openCloseTime
        clearTooltip()
    }

    /**
     * Do not modify ui.handler.openCloseCounter here.
     */
    open fun endOpening(delta: Float) {
        handler.opacity = 1f
        clearTooltip()
    }

    /**
     * Do not modify ui.handler.openCloseCounter here.
     */
    open fun endClosing(delta: Float) {
        handler.opacity = 0f
        clearTooltip()
    }

    abstract override fun dispose()

    fun addUIitem(uiItem: UIItemisable) {
        if (!uiItems.contains(uiItem)) uiItems.add(uiItem)
    }

    fun addUIitemAtHead(uiItem: UIItemisable) {
        if (!uiItems.contains(uiItem)) uiItems.add(0, uiItem)
    }

    fun removeUIitem(uiItem: UIItemisable) {
        uiItems.remove(uiItem)
    }

    fun mouseInScreen(x: Int, y: Int) = x in 0 until App.scr.windowW && y in 0 until App.scr.windowH

    /**
     * Called by the screen's InputProcessor
     *
     * When implementing this, make sure to use ```mouseInScreen()``` function!
     */
    open fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.touchDragged(screenX, screenY, pointer) }
            handler.subUIs.forEach { it.touchDragged(screenX, screenY, pointer) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (this.isVisible && mouseInScreen(screenX, screenY) && !openingClickLatched) {
            uiItems.forEach { it.touchDown(screenX, screenY, pointer, button) }
            handler.subUIs.forEach { it.touchDown(screenX, screenY, pointer, button) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.touchUp(screenX, screenY, pointer, button) }
            handler.subUIs.forEach { it.touchUp(screenX, screenY, pointer, button) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.scrolled(amountX, amountY) }
            handler.subUIs.forEach { it.scrolled(amountX, amountY) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun keyDown(keycode: Int): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.keyDown(keycode) }
            handler.subUIs.forEach { it.keyDown(keycode) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun keyUp(keycode: Int): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.keyUp(keycode) }
            handler.subUIs.forEach { it.keyUp(keycode) }
            return true
        }
        else return false
    }
    /** Called by the screen's InputProcessor */
    open fun keyTyped(character: Char): Boolean {
        // TODO process key typing from the virtual keyboard?

        return false
    }
    open fun inputStrobed(e: TerrarumKeyboardEvent) {
        if (this.isVisible) {
            uiItems.forEach { it.inputStrobed(e) }
            handler.subUIs.forEach { it.inputStrobed(e) }
        }
    }

    open fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }


    // handler func aliases //

    open fun setPosition(x: Int, y: Int) {
        handler.setPosition(x, y)
    }

    fun setAsAlwaysVisible() {
        handler.setAsAlwaysVisible()
    }

    open fun setAsOpen() {
        handler.setAsOpen()
    }

    open fun setAsClose() {
        handler.setAsClose()
    }

    open fun toggleOpening() {
//        handler.toggleOpening()
        if (handler.alwaysVisible && !handler.doNotWarnConstant) {
            throw RuntimeException("[UIHandler] Tried to 'toggle opening of' constant UI")
        }
        if (isVisible) {
            if (!isClosing) {
                setAsClose()
            }
        }
        else {
            if (!isOpening) {
                setAsOpen()
            }
        }
    }

    inline val isOpened: Boolean
        get() = handler.isOpened

    inline val isOpening: Boolean
        get() = handler.isOpening

    inline val isClosing: Boolean
        get() = handler.isClosing

    inline val isClosed: Boolean
        get() = handler.isClosed

    inline var opacity: Float
        get() = handler.opacity
        set(value) { handler.opacity = value }

    inline var scale: Float
        get() = handler.scale
        set(value) { handler.scale = value }

    inline val isTakingControl: Boolean
        get() = handler.isTakingControl

    inline var isVisible: Boolean
        get() = handler.isVisible
        set(value) { handler.isVisible = value }

    // end of handler func aliases

    init {
//        if (uiItems.isEmpty()) println("UICanvas '${this.javaClass.name}' has no UIItem registered, just so you know...")
    }

    companion object {
        const val OPENCLOSE_GENERIC = 0.1f

        fun doOpeningFade(ui: UICanvas, openCloseTime: Second) {
            ui.handler.opacity = max(0f, ui.handler.openCloseCounter - 0.02f) / openCloseTime // fade start 1/50 sec late, it's intended
        }
        fun doClosingFade(ui: UICanvas, openCloseTime: Second) {
            ui.handler.opacity = (openCloseTime - ui.handler.openCloseCounter) / openCloseTime
        }
        fun endOpeningFade(ui: UICanvas) {
            ui.handler.opacity = 1f
        }
        fun endClosingFade(ui: UICanvas) {
            ui.handler.opacity = 0f
        }


        fun doOpeningPopOut(ui: UICanvas, openCloseTime: Second, position: Position) {
            when (position) {
                Position.LEFT -> ui.handler.posX = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        -ui.width.toFloat(),
                        0f
                ).roundToInt()
                Position.TOP -> ui.handler.posY = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        -ui.height.toFloat(),
                        0f
                ).roundToInt()
                Position.RIGHT -> ui.handler.posX = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        App.scr.wf,
                        App.scr.width - ui.width.toFloat()
                ).roundToInt()
                Position.BOTTOM -> ui.handler.posY = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        App.scr.hf,
                        App.scr.height - ui.height.toFloat()
                ).roundToInt()
            }
        }
        fun doClosingPopOut(ui: UICanvas, openCloseTime: Second, position: Position) {
            when (position) {
                Position.LEFT -> ui.handler.posX = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        0f,
                        -ui.width.toFloat()
                ).roundToInt()
                Position.TOP -> ui.handler.posY = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        0f,
                        -ui.height.toFloat()
                ).roundToInt()
                Position.RIGHT -> ui.handler.posX = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        App.scr.width - ui.width.toFloat(),
                        App.scr.wf
                ).roundToInt()
                Position.BOTTOM -> ui.handler.posY = Movement.fastPullOut(
                        ui.handler.openCloseCounter / openCloseTime,
                        App.scr.height - ui.height.toFloat(),
                        App.scr.hf
                ).roundToInt()
            }
        }
        fun endOpeningPopOut(ui: UICanvas, position: Position) {
            when (position) {
                Position.LEFT -> ui.handler.posX = 0
                Position.TOP -> ui.handler.posY = 0
                Position.RIGHT -> ui.handler.posX = App.scr.width - ui.width
                Position.BOTTOM -> ui.handler.posY = App.scr.height - ui.height
            }
        }
        fun endClosingPopOut(ui: UICanvas, position: Position) {
            when (position) {
                Position.LEFT -> ui.handler.posX = -ui.width
                Position.TOP -> ui.handler.posY = -ui.height
                Position.RIGHT -> ui.handler.posX = App.scr.width
                Position.BOTTOM -> ui.handler.posY = App.scr.height
            }
        }

        // TODO add blackboard take in/out (sinusoidal)

        enum class Position {
            LEFT, RIGHT, TOP, BOTTOM
        }
    }

    override fun toString(): String = "${this.javaClass.simpleName}@${this.hashCode().toString(16)}"
}

/**
 * Annotation added to the UI that is not meant to have a user interaction in any way, so that even if
 * the mouse cursor is above it, the UI is not recognised as such. This is useful on UIs that spawn a
 * tooltip and the said UI is checking if the mouse cursor is NOT above any other overlaid UI (e.g. a message box)
 */
annotation class UINotControllable