package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gamecontroller.KeyToggler

/**
 * UIHandler is a handler for UICanvas. It opens/closes the attached UI, moves the "window" (or "canvas")
 * to the coordinate of displayed cartesian coords, and update and render the UI.
 * It also process game inputs and send control events to the UI so that the UI can handle them.
 *
 * Newly created UI is invisible by default.
 *
 * Created by minjaesong on 15-12-31.
 */
class UIHandler(val UI: UICanvas,
                val toggleKey: Int? = null, val toggleButton: Int? = null,
                // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
                var customPositioning: Boolean = false,
                val doNotWarnConstant: Boolean = false
) {

    // X/Y Position relative to the game window.
    var posX: Int = 0
    var posY: Int = 0

    private var alwaysVisible = false

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
    var scale = 1f

    var openCloseCounter = 0f

    init {
        UI.handler = this
    }


    fun update(delta: Float) {
        // open/close UI by key pressed
        if (toggleKey != null) {
            if (KeyToggler.isOn(toggleKey)) {
                setAsOpen()
            }
            else {
                setAsClose()
            }
        }
        if (toggleButton != null) {
            /* */
        }


        if (openFired && openCloseCounter > 9) openFired = false
        if (closeFired && openCloseCounter > 9) closeFired = false


        if (isVisible || alwaysVisible) {
            UI.update(delta)
        }

        if (isOpening) {
            isVisible = true
            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (open)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < UI.openCloseTime) {
                UI.doOpening(delta)
                // println("UIHandler.opening ${UI.javaClass.simpleName}")
            }
            else {
                UI.endOpening(delta)
                isOpening = false
                isClosing = false
                isOpened = true
                openCloseCounter = 0f
            }
        }
        else if (isClosing) {
            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (close)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < UI.openCloseTime) {
                UI.doClosing(delta)
                // println("UIHandler.closing ${UI.javaClass.simpleName}")
            }
            else {
                UI.endClosing(delta)
                isClosing = false
                isOpening = false
                isOpened = false
                isVisible = false
                openCloseCounter = 0f
            }
        }
    }

    fun render(batch: SpriteBatch) {
        if (isVisible || alwaysVisible) {

            batch.projectionMatrix = Matrix4()

            if (!customPositioning)
                TerrarumGDX.ingame?.camera?.position?.set(posX.toFloat(), posY.toFloat(), 0f) // does it work?

            UI.render(batch)
            //ingameGraphics.flush()
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
     * Send OPEN signal to the attached UI.
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
     * Send CLOSE signal to the attached UI.
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

    fun processInput(delta: Float) {
        if (isVisible) {
            UI.processInput(delta)
        }
    }

    fun keyPressed(key: Int, c: Char) {
        if (isVisible && UI is KeyControlled) {
            UI.keyPressed(key, c)
        }
    }

    fun keyReleased(key: Int, c: Char) {
        if (isVisible && UI is KeyControlled) {
            UI.keyReleased(key, c)
        }
    }

    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        if (isVisible && UI is MouseControlled) {
            UI.mouseMoved(oldx, oldy, newx, newy)
        }
    }

    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        if (isVisible && UI is MouseControlled) {
            UI.mouseDragged(oldx, oldy, newx, newy)
        }
    }

    fun mousePressed(button: Int, x: Int, y: Int) {
        if (isVisible && UI is MouseControlled) {
            UI.mousePressed(button, x, y)
        }
    }

    fun mouseReleased(button: Int, x: Int, y: Int) {
        if (isVisible && UI is MouseControlled) {
            UI.mouseReleased(button, x, y)
        }
    }

    fun mouseWheelMoved(change: Int) {
        if (isVisible && UI is MouseControlled) {
            UI.mouseWheelMoved(change)
        }
    }

    fun controllerButtonPressed(controller: Int, button: Int) {
        if (isVisible && UI is KeyControlled) {
            UI.controllerButtonPressed(controller, button)
        }
    }

    fun controllerButtonReleased(controller: Int, button: Int) {
        if (isVisible && UI is KeyControlled) {
            UI.controllerButtonReleased(controller, button)
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
}
