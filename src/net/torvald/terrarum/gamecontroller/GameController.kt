package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.worlddrawer.WorldCamera
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
object GameController {

    private val ingame = Terrarum.ingame!!
    
    // these four values can also be accessed with GameContainer.<varname>
    // e.g.  gc.mouseTileX

    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
    val mouseX: Float
        get() = (WorldCamera.x + Terrarum.appgc.input.mouseX / (ingame.screenZoom ?: 1f))
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    val mouseY: Float
        get() = (WorldCamera.y + Terrarum.appgc.input.mouseY / (ingame.screenZoom ?: 1f))
    /** currently pointing tile coordinate */
    val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()

    fun processInput(gc: GameContainer, delta: Int, input: Input) {
        // actor process input
        if (!ingame.consoleHandler.isTakingControl) {
            if (ingame.canPlayerControl) {
                ingame.actorContainer.forEach {
                    if (it is Controllable) {
                        // disable control of actor if the actor is riding something?
                        if ((it as ActorHumanoid).vehicleRiding != null) {
                            it.vehicleRiding!!.processInput(gc, delta, input)
                        }
                        else {
                            it.processInput(gc, delta, input)
                        }
                    }
                }
            }
            else {
                ingame.uiContainer.forEach {
                    it.processInput(gc, delta, input)
                }
            }
        }
        else {
            ingame.consoleHandler.processInput(gc, delta, input)
        }


        ///////////////////
        // MOUSE CONTROL //
        ///////////////////

        // Use item: assuming the player has only one effective grip (EquipPosition.HAND_GRIP)
        if (ingame.player != null && ingame.canPlayerControl) {
            if (input.isMouseButtonDown(Terrarum.getConfigInt("mouseprimary")) || input.isMouseButtonDown(Terrarum.getConfigInt("mousesecondary"))) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                itemOnGrip?.let {
                    if (input.isMouseButtonDown(Terrarum.getConfigInt("mouseprimary"))) {
                        ingame.player!!.consumePrimary(it)
                    }
                    if (input.isMouseButtonDown(Terrarum.getConfigInt("mousesecondary"))) {
                        ingame.player!!.consumeSecondary(it)
                    }
                }
            }
        }


        /////////////////////
        // GAMEPAD CONTROL //
        /////////////////////
    }

    fun keyPressed(key: Int, c: Char) {

        
        
        
        if (ingame.canPlayerControl) {
            ingame.player?.keyPressed(key, c)
        }

        if (Terrarum.getConfigIntArray("keyquickselalt").contains(key)
            || key == Terrarum.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsOpen()
            ingame.uiQuickBar.setAsClose()
        }

        ingame.uiContainer.forEach { it.keyPressed(key, c) } // for KeyboardControlled UIcanvases
    }

    fun keyReleased(key: Int, c: Char) {

        if (Terrarum.getConfigIntArray("keyquickselalt").contains(key)
            || key == Terrarum.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsClose()
            ingame.uiQuickBar.setAsOpen()
        }

        ingame.uiContainer.forEach { it.keyReleased(key, c) } // for KeyboardControlled UIcanvases
    }

    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        ingame.uiContainer.forEach { it.mouseMoved(oldx, oldy, newx, newy) } // for MouseControlled UIcanvases
    }

    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        ingame.uiContainer.forEach { it.mouseDragged(oldx, oldy, newx, newy) } // for MouseControlled UIcanvases
    }

    fun mousePressed(button: Int, x: Int, y: Int) {
        ingame.uiContainer.forEach { it.mousePressed(button, x, y) } // for MouseControlled UIcanvases
    }

    fun mouseReleased(button: Int, x: Int, y: Int) {
        if (Terrarum.ingame != null) {
            val ingame = Terrarum.ingame!!
            // don't separate Player from this! Physics will break, esp. airborne manoeuvre
            if (ingame.player != null && ingame.canPlayerControl) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                if (itemOnGrip != null) {
                    if (button == Terrarum.getConfigInt("mousePrimary")) {
                        itemOnGrip.endPrimaryUse(Terrarum.appgc, Terrarum.delta)
                    }
                    if (button == Terrarum.getConfigInt("mouseSecondary")) {
                        itemOnGrip.endSecondaryUse(Terrarum.appgc, Terrarum.delta)
                    }
                }
            }


            ingame.uiContainer.forEach { it.mouseReleased(button, x, y) } // for MouseControlled UIcanvases
        }
    }

    fun mouseWheelMoved(change: Int) {
        ingame.uiContainer.forEach { it.mouseWheelMoved(change) } // for MouseControlled UIcanvases
    }

    fun controllerButtonPressed(controller: Int, button: Int) {
        ingame.uiContainer.forEach { it.controllerButtonPressed(controller, button) } // for GamepadControlled UIcanvases
    }

    fun controllerButtonReleased(controller: Int, button: Int) {
        ingame.uiContainer.forEach { it.controllerButtonReleased(controller, button) } // for GamepadControlled UIcanvases
    }
}

/** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
val GameContainer.mouseX: Double
    get() = GameController.mouseX.toDouble()
/** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
val GameContainer.mouseY: Double
    get() = GameController.mouseY.toDouble()
/** currently pointing tile coordinate */
val GameContainer.mouseTileX: Int
    get() = GameController.mouseTileX
/** currently pointing tile coordinate */
val GameContainer.mouseTileY: Int
    get() = GameController.mouseTileY
val GameContainer.mouseScreenX: Int
    get() = Terrarum.appgc.input.mouseX
val GameContainer.mouseScreenY: Int
    get() = Terrarum.appgc.input.mouseY
