package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.FlippingSpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * UIHandler is a handler for UICanvas. It opens/closes the attached UI, moves the "window" (or "canvas")
 * to the coordinate of displayed cartesian coords, and update and render the UI.
 * It also process game inputs and send control events to the UI so that the UI can handle them.
 *
 * The UI is *non-compositing* and thus has no underlying framebuffers, meaning that some of the effects (opacity, scaling)
 * must be separately implemented onto the UICanvas (which may cause some artefacts when UI elements are overlapping and they are both semi-transparent)
 *
 * New UIs are NORMALLY HIDDEN; set it visible as you need!
 *
 * Created by minjaesong on 2015-12-31.
 */
class UIHandler(//var UI: UICanvas,
        var toggleKeyLiteral: String? = null, // string key of the config
        var toggleButtonLiteral: String? = null, // string key of the config
                // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        var customPositioning: Boolean = false, // mainly used by vital meter
        var doNotWarnConstant: Boolean = false,
        internal var allowESCtoClose: Boolean = true,
        var uiTogglerFunctionDefault: ((UIHandler) -> Unit)? = null
): Disposable {

    companion object {
        private val SHADER_PROG_FRAG = """
#version 150
#ifdef GL_ES
    precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float opacity;

out vec4 fragColor;

void main(void) {
    vec4 color = texture(u_texture, v_texCoords).rgba;
    
    fragColor = v_color * vec4(color.rgb, color.a * opacity);
}
""".trimIndent()

        private val SHADER_PROG_VERT = """
#version 150

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
        """.trimIndent()
    }

    // X/Y Position relative to the game window.
    var posX: Int = 0
    var posY: Int = 0

    var initialX = posX
    var initialY = posY

    var alwaysVisible = false; private set

    var isOpening = false
    var isClosing = false
    var isOpened = false // fully opened
    var isVisible: Boolean = false
        get() = if (alwaysVisible) true
                else field
        set(value) {
            if (alwaysVisible)
                throw RuntimeException("[UIHandler] Tried to 'set visibility of' constant UI")
            if (value == true) {
                isOpened = true
                field = value
            }
            else {
                isOpened = false
                field = value
            }
        }

    /**
     * being TRUE for only one frame when the UI is told to open
     */
    var openFired = false
    var closeFired = false

    var opacity = 1f
        /*set(value) {
            field = value
            opacityColour.a = value
        }*/
    var scale = 1f

    //val opacityColour = Color(1f, 1f, 1f, opacity)

    var openCloseCounter = 0f

    private val shader = App.loadShaderInline(SHADER_PROG_VERT, SHADER_PROG_FRAG)

    var uiToggleLocked = false; private set

    init {
        //UI.handler = this
    }

    // getConfigInt(toggleKeyLiteral)
    val toggleKey: Int?; get() = App.getConfigInt(toggleKeyLiteral).let { if (it == -1) null else it } // to support in-screen keybind changing
    // getConfigInt(toggleButtonLiteral)
    val toggleButton: Int?; get() = App.getConfigInt(toggleButtonLiteral).let { if (it == -1) null else it } // to support in-screen keybind changing


    val toggleKeyExtra: ArrayList<() -> Int> = arrayListOf()

    /**
     * Takes a function that works with UIHandler.
     * For the function, try starting from the:
     * ```
     * if (it.isClosed)
     *     it.setAsOpen()
     * else if (it.isOpened)
     *     setAsClose()
     * ```
     */
    val toggleKeyExtraAction: ArrayList<(UIHandler) -> Unit> = arrayListOf()


    val subUIs = ArrayList<UICanvas>()

    val mouseUp: Boolean
        get() {
            for (k in subUIs.indices) {
                val ret2 = subUIs[k].mouseUp
                if (ret2) return true
            }
            return false
        }

    fun addSubUI(ui: UICanvas) {
        if (subUIs.contains(ui))
            throw IllegalArgumentException(
                    "Exact copy of the UI already exists: The instance of $ui"
            )

        subUIs.add(ui)
    }

    fun removeSubUI(ui: UICanvas) {
        subUIs.remove(ui)
    }

    fun lockToggle() {
        uiToggleLocked = true
    }
    fun unlockToggle() {
        uiToggleLocked = false
    }


    fun update(ui: UICanvas, delta: Float) {
        // open/close UI by key pressed
        // some UIs will pause the game, and they still need to be closed
        if (!uiToggleLocked && (Terrarum.ingame?.consoleOpened == false && (Terrarum.ingame?.paused == false || isOpened))) {
            if (toggleKey != null && Gdx.input.isKeyJustPressed(toggleKey!!)) {
                if (uiTogglerFunctionDefault == null) {
                    if (isClosed)
                        setAsOpen()
                    else if (isOpened)
                        setAsClose()
                }
                else uiTogglerFunctionDefault!!.invoke(this)

                // for the case of intermediate states, do nothing.
            }
            if (toggleButton != null && Gdx.input.isButtonJustPressed(toggleButton!!)) {
                if (uiTogglerFunctionDefault == null) {
                    if (isClosed)
                        setAsOpen()
                    else if (isOpened)
                        setAsClose()
                }
                else uiTogglerFunctionDefault!!.invoke(this)
            }

            toggleKeyExtra.forEachIndexed { index, getKey ->
                if (Gdx.input.isKeyJustPressed(getKey())) {
                    toggleKeyExtraAction[index].invoke(this)
                }
            }

            // ESC is a master key for closing
            if (!alwaysVisible && allowESCtoClose && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && isOpened) {
                setAsClose()
            }
        }

        //if (openFired && openCloseCounter > 9) openFired = false
        //if (closeFired && openCloseCounter > 9) closeFired = false


        if (isVisible) {
            ui.updateUI(delta)
        }

        if (isOpening) {
            openFired = false

            isVisible = true
            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (open)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < ui.openCloseTime) {
                ui.doOpening(delta)
                // println("UIHandler.opening ${UI.javaClass.simpleName}")
            }
            else {
                ui.doOpening(0f)
                ui.endOpening(delta)
                isOpening = false
                isClosing = false
                isOpened = true
                openCloseCounter = 0f
            }
        }
        else if (isClosing) {
            closeFired = false

            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (close)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < ui.openCloseTime) {
                ui.doClosing(delta)
                // println("UIHandler.closing ${UI.javaClass.simpleName}")
            }
            else {
                ui.doClosing(0f)
                ui.endClosing(delta)
                isClosing = false
                isOpening = false
                isOpened = false
                isVisible = false
                openCloseCounter = 0f
            }
        }


        subUIs.forEach { it.update(delta) }
    }

    fun render(ui: UICanvas, batch: SpriteBatch, camera: Camera, parentOpacity: Float) {

        if (isVisible) {
            // camera SHOULD BE CENTERED to HALFX and HALFY (see StateInGame) //


            //batch.projectionMatrix = Matrix4()
//
            //if (!customPositioning)
            //    Terrarum.ingame?.camera?.position?.set(posX.toFloat(), posY.toFloat(), 0f) // does it work?



            if (!customPositioning) {
                setCameraPosition(batch, camera, posX.toFloat(), posY.toFloat())
            }
            batch.color = Color.WHITE

            batch.shader = shader
            shader.setUniformf("opacity", opacity * parentOpacity)
            ui.renderUI(batch, camera)
            //ingameGraphics.flush()

            batch.shader = null
            batch.color = Color.WHITE


            setCameraPosition(batch, camera, 0f, 0f)
        }


        subUIs.forEach {
            it.render(batch, camera)
            batch.color = Color.WHITE
        }

    }

    fun setPosition(x: Int, y: Int) {
        posX = x
        posY = y
    }

    fun setAsAlwaysVisible() {
        isVisible = true
        alwaysVisible = true
        isOpened = true
        isOpening = false
        isClosing = false
    }

    /**
     * Send OPEN signal to the attached UI. The actual job is done when the handler is being updated.
     */
    fun setAsOpen() {
        if (alwaysVisible && !doNotWarnConstant) {
            throw RuntimeException("[UIHandler] Tried to 'open' constant UI")
        }
        if (!isOpened && !isOpening) {
            isOpened = false
            isOpening = true
            isClosing = false
            isVisible = true

            openFired = true
        }
    }

    /**
     * Send CLOSE signal to the attached UI. The actual job is done when the handler is being updated.
     */
    fun setAsClose() {
        if (alwaysVisible && !doNotWarnConstant) {
            throw RuntimeException("[UIHandler] Tried to 'close' constant UI")
        }
        if ((isOpening || isOpened) && !isClosing && isVisible) {
            isOpened = false
            isClosing = true
            isOpening = false

            closeFired = true
        }
    }

    val isClosed: Boolean
        get() = !isOpened && !isClosing && !isOpening

    fun toggleOpening() {
        if (alwaysVisible && !doNotWarnConstant) {
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

    // constant UI can't take control
    val isTakingControl: Boolean
        get() {
            if (alwaysVisible) {
                return false
            }
            return isVisible && !isOpening
        }

    fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
        TerrarumIngame.setCameraPosition(batch, camera, newX, newY)
    }

    fun touchDragged(uiItems: List<UIItem>, screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (isVisible) {
            uiItems.forEach { it.touchDragged(screenX, screenY, pointer) }
            subUIs.forEach { it.touchDragged(screenX, screenY, pointer) }
            return true
        }
        else {
            return false
        }
    }
    fun touchDown(uiItems: List<UIItem>, screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (isVisible) {
            uiItems.forEach { it.touchDown(screenX, screenY, pointer, button) }
            subUIs.forEach { it.touchDown(screenX, screenY, pointer, button) }
            return true
        }
        else {
            return false
        }
    }
    fun touchUp(uiItems: List<UIItem>, screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (isVisible) {
            uiItems.forEach { it.touchUp(screenX, screenY, pointer, button) }
            subUIs.forEach { it.touchUp(screenX, screenY, pointer, button) }
            return true
        }
        else {
            return false
        }
    }
    fun scrolled(uiItems: List<UIItem>, amountX: Float, amountY: Float): Boolean {
        if (isVisible) {
            uiItems.forEach { it.scrolled(amountX, amountY) }
            subUIs.forEach { it.scrolled(amountX, amountY) }
            return true
        }
        else {
            return false
        }
    }
    fun keyDown(uiItems: List<UIItem>, keycode: Int): Boolean {
        if (isVisible) {
            uiItems.forEach { it.keyDown(keycode) }
            subUIs.forEach { it.keyDown(keycode) }
            return true
        }
        else {
            return false
        }
    }
    fun keyUp(uiItems: List<UIItem>, keycode: Int): Boolean {
        if (isVisible) {
            uiItems.forEach { it.keyUp(keycode) }
            subUIs.forEach { it.keyUp(keycode) }
            return true
        }
        else {
            return false
        }
    }
    fun keyTyped(uiItems: List<UIItem>, character: Char): Boolean {
        if (isVisible) {
            //uiItems.forEach { it.keyTyped(character) }
            subUIs.forEach { it.keyTyped(character) }
            return true
        }
        else {
            return false
        }
    }

    /** Don't dispose common assets, this function is called when the ingame does hide() */
    override fun dispose() {
        toggleKey?.let { KeyToggler.forceSet(it, false) }
        toggleButton?.let { /* ButtonToggler.forceSet(it, false) */ }
        shader.dispose()
    }
}
