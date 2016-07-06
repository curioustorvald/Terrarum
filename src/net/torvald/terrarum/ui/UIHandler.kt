package net.torvald.terrarum.ui

import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.Terrarum
import com.jme3.math.FastMath
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame

/**
 * UIHandler is a handler for UICanvas. It opens/closes the attached UI, moves the "window" (or "canvas")
 * to the coordinate of displayed cartesian coords, and update and render the UI.
 * It also process game inputs and send control events to the UI so that the UI can handle them.
 *
 * Created by minjaesong on 15-12-31.
 */
class UIHandler
/**
 * Construct new UIHandler with given UI attached.
 * Invisible in default.
 * @param UI
 * *
 * @throws SlickException
 */
@Throws(SlickException::class)
constructor(val UI: UICanvas) {

    // X/Y Position to the game window.
    var posX: Int = 0
        private set
    var posY: Int = 0
        private set

    private var alwaysVisible = false

    private val UIGraphicInstance: Graphics
    private val UIDrawnCanvas: Image

    private var opening = false
    private var closing = false
    private var opened = false // fully opened
    var visible: Boolean = false
        get() = if (alwaysVisible) true
                else field
        set(value) {
            if (alwaysVisible)
                throw RuntimeException("[UIHandler] Tried to 'set visibility of' constant UI")

            field = value
        }

    var openCloseCounter = 0

    init {
        println("[UIHandler] Creating UI '${UI.javaClass.simpleName}'")

        UIDrawnCanvas = Image(
                FastMath.nearestPowerOfTwo(UI.width), FastMath.nearestPowerOfTwo(UI.height))

        UIGraphicInstance = UIDrawnCanvas.graphics
    }


    fun update(gc: GameContainer, delta: Int) {
        if (visible || alwaysVisible) {
            UI.update(gc, delta)
        }

        if (opening) {
            visible = true
            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (open)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < UI.openCloseTime) {
                UI.doOpening(gc, delta)
                // println("UIHandler.opening ${UI.javaClass.simpleName}")
            }
            else {
                UI.endOpening(gc, delta)
                opening = false
                opened = true
                openCloseCounter = 0
            }
        }
        else if (closing) {
            openCloseCounter += delta

            // println("UI ${UI.javaClass.simpleName} (close)")
            // println("-> timecounter $openCloseCounter / ${UI.openCloseTime} timetakes")

            if (openCloseCounter < UI.openCloseTime) {
                UI.doClosing(gc, delta)
                // println("UIHandler.closing ${UI.javaClass.simpleName}")
            }
            else {
                UI.endClosing(gc, delta)
                closing = false
                opened = false
                visible = false
                openCloseCounter = 0
            }
        }
    }

    fun render(gc: GameContainer, sbg: StateBasedGame, gameGraphicInstance: Graphics) {
        if (visible || alwaysVisible) {
            UIGraphicInstance.clear()
            UIGraphicInstance.font = Terrarum.gameFont

            UI.render(gc, UIGraphicInstance)
            if (sbg.currentStateID == Terrarum.SCENE_ID_GAME) {
                gameGraphicInstance.drawImage(UIDrawnCanvas,
                        posX + MapCamera.cameraX * Terrarum.ingame.screenZoom,
                        posY + MapCamera.cameraY * Terrarum.ingame.screenZoom
                )// compensate for screenZoom AND camera translation
                // (see Game.render -> g.translate())
            }
            else {
                gameGraphicInstance.drawImage(UIDrawnCanvas,
                        posX.toFloat(),
                        posY.toFloat()
                )
            }
        }
    }

    fun setPosition(x: Int, y: Int) {
        posX = x
        posY = y
    }

    fun setAsAlwaysVisible() {
        alwaysVisible = true
        visible = true
        opened = true
        opening = false
        closing = false
    }

    /**
     * Send OPEN signal to the attached UI.
     */
    fun setAsOpening() {
        if (alwaysVisible) {
            throw RuntimeException("[UIHandler] Tried to 'open' constant UI")
        }
        opened = false
        opening = true
    }

    /**
     * Send CLOSE signal to the attached UI.
     */
    fun setAsClosing() {
        if (alwaysVisible) {
            throw RuntimeException("[UIHandler] Tried to 'close' constant UI")
        }
        opened = false
        closing = true
    }

    fun toggleOpening() {
        if (alwaysVisible) {
            throw RuntimeException("[UIHandler] Tried to 'toggle opening of' constant UI")
        }
        if (visible) {
            if (!closing) {
                setAsClosing()
            }
        }
        else {
            if (!opening) {
                setAsOpening()
            }
        }
    }

    fun processInput(input: Input) {
        if (visible) {
            UI.processInput(input)
        }
    }

    fun keyPressed(key: Int, c: Char) {
        if (visible && UI is UITypable) {
            UI.keyPressed(key, c)
        }
    }

    fun keyReleased(key: Int, c: Char) {
        if (visible && UI is UITypable) {
            UI.keyReleased(key, c)
        }
    }

    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        if (visible && UI is UIClickable) {
            UI.mouseMoved(oldx, oldy, newx, newy)
        }
    }

    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        if (visible && UI is UIClickable) {
            UI.mouseDragged(oldx, oldy, newx, newy)
        }
    }

    fun mousePressed(button: Int, x: Int, y: Int) {
        if (visible && UI is UIClickable) {
            UI.mousePressed(button, x, y)
        }
    }

    fun mouseReleased(button: Int, x: Int, y: Int) {
        if (visible && UI is UIClickable) {
            UI.mouseReleased(button, x, y)
        }
    }

    fun mouseWheelMoved(change: Int) {
        if (visible && UI is UIClickable) {
            UI.mouseWheelMoved(change)
        }
    }

    fun controllerButtonPressed(controller: Int, button: Int) {
        if (visible && UI is UIClickable) {
            UI.controllerButtonPressed(controller, button)
        }
    }

    fun controllerButtonReleased(controller: Int, button: Int) {
        if (visible && UI is UIClickable) {
            UI.controllerButtonReleased(controller, button)
        }
    }

    // constant UI can't take control
    val isTakingControl: Boolean
        get() {
            if (alwaysVisible) {
                return false
            }
            return visible && !opening
        }
}
