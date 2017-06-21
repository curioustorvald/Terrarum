package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 15-12-31.
 */
object GameController {

    private val ingame = TerrarumGDX.ingame!!
    
    // these four values can also be accessed with GameContainer.<varname>
    // e.g.  gc.mouseTileX

    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
    val mouseX: Float
        get() = (WorldCamera.x + TerrarumGDX.mouseX.toFloat() / (ingame.screenZoom ?: 1f))
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    val mouseY: Float
        get() = (WorldCamera.y + TerrarumGDX.mouseY.toFloat() / (ingame.screenZoom ?: 1f))
    /** currently pointing tile coordinate */
    val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()

    fun processInput(delta: Float) {
        // actor process input
        if (!ingame.consoleHandler.isTakingControl) {
            if (ingame.canPlayerControl) {
                ingame.actorContainer.forEach {
                    if (it is Controllable) {
                        // disable control of actor if the actor is riding something?
                        if ((it as ActorHumanoid).vehicleRiding != null) {
                            it.vehicleRiding!!.processInput(delta)
                        }
                        else {
                            it.processInput(delta)
                        }
                    }
                }
            }
            else {
                ingame.uiContainer.forEach {
                    it.processInput(delta)
                }
            }
        }
        else {
            ingame.consoleHandler.processInput(delta)
        }


        ///////////////////
        // MOUSE CONTROL //
        ///////////////////

        // Use item: assuming the player has only one effective grip (EquipPosition.HAND_GRIP)
        if (ingame.player != null && ingame.canPlayerControl) {
            if (Gdx.input.isButtonPressed(TerrarumGDX.getConfigInt("mouseprimary")) || Gdx.input.isButtonPressed(TerrarumGDX.getConfigInt("mousesecondary"))) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                itemOnGrip?.let {
                    if (Gdx.input.isButtonPressed(TerrarumGDX.getConfigInt("mouseprimary"))) {
                        ingame.player!!.consumePrimary(it)
                    }
                    if (Gdx.input.isButtonPressed(TerrarumGDX.getConfigInt("mousesecondary"))) {
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

        if (TerrarumGDX.getConfigIntArray("keyquickselalt").contains(key)
            || key == TerrarumGDX.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsOpen()
            ingame.uiQuickBar.setAsClose()
        }

        ingame.uiContainer.forEach { it.keyPressed(key, c) } // for KeyboardControlled UIcanvases
    }

    fun keyReleased(key: Int, c: Char) {

        if (TerrarumGDX.getConfigIntArray("keyquickselalt").contains(key)
            || key == TerrarumGDX.getConfigInt("keyquicksel")) {
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
        if (TerrarumGDX.ingame != null) {
            val ingame = TerrarumGDX.ingame!!
            // don't separate Player from this! Physics will break, esp. airborne manoeuvre
            if (ingame.player != null && ingame.canPlayerControl) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                if (itemOnGrip != null) {
                    if (button == TerrarumGDX.getConfigInt("mousePrimary")) {
                        itemOnGrip.endPrimaryUse(Gdx.graphics.deltaTime)
                    }
                    if (button == TerrarumGDX.getConfigInt("mouseSecondary")) {
                        itemOnGrip.endSecondaryUse(Gdx.graphics.deltaTime)
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

/* // Use TerrarumGDX.*
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
    get() = Terrarum.appgc.input.mouseY*/


