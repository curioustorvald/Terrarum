package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 15-12-31.
 */
object GameController : InputAdapter() {



    private val ingame = Terrarum.ingame!!
    
    // these four values can also be accessed with GameContainer.<varname>
    // e.g.  gc.mouseTileX

    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
    val mouseX: Float
        get() = WorldCamera.x + Gdx.input.x / (ingame.screenZoom)
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    val mouseY: Float
        get() = WorldCamera.y + Gdx.input.y / (ingame.screenZoom)
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
            if (Gdx.input.isButtonPressed(Terrarum.getConfigInt("mouseprimary")) || Gdx.input.isButtonPressed(Terrarum.getConfigInt("mousesecondary"))) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                itemOnGrip?.let {
                    if (Gdx.input.isButtonPressed(Terrarum.getConfigInt("mouseprimary"))) {
                        ingame.player!!.consumePrimary(it)
                    }
                    if (Gdx.input.isButtonPressed(Terrarum.getConfigInt("mousesecondary"))) {
                        ingame.player!!.consumeSecondary(it)
                    }
                }
            }
        }


        /////////////////////
        // GAMEPAD CONTROL //
        /////////////////////
    }

    override fun keyDown(keycode: Int): Boolean {

        if (ingame.canPlayerControl) {
            ingame.player?.keyDown(keycode)
        }

        if (Terrarum.getConfigIntArray("keyquickselalt").contains(keycode)
            || keycode == Terrarum.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsOpen()
            ingame.uiQuickBar.setAsClose()
        }

        ingame.uiContainer.forEach { it.keyDown(keycode) } // for KeyboardControlled UIcanvases

        // Debug UIs
        if (keycode == Input.Keys.F3) {
            ingame.debugWindow.toggleOpening()
        }
        if (keycode == Input.Keys.GRAVE) {
            ingame.consoleHandler.toggleOpening()
        }


        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        if (Terrarum.getConfigIntArray("keyquickselalt").contains(keycode)
            || keycode == Terrarum.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsClose()
            ingame.uiQuickBar.setAsOpen()
        }

        ingame.uiContainer.forEach { it.keyUp(keycode) } // for KeyboardControlled UIcanvases


        return true
    }

    override fun keyTyped(character: Char): Boolean {
        ingame.uiContainer.forEach { it.keyTyped(character) }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        ingame.uiContainer.forEach { it.mouseMoved(screenX, screenY) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (Terrarum.ingame != null) {
            val ingame = Terrarum.ingame!!
            // don't separate Player from this! Physics will break, esp. airborne manoeuvre
            if (ingame.player != null && ingame.canPlayerControl) {
                val itemOnGrip = ingame.player!!.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                if (itemOnGrip != null) {
                    if (button == Terrarum.getConfigInt("mouseprimary")) {
                        itemOnGrip.endPrimaryUse(Gdx.graphics.deltaTime)
                    }
                    if (button == Terrarum.getConfigInt("mousesecondary")) {
                        itemOnGrip.endSecondaryUse(Gdx.graphics.deltaTime)
                    }
                }
            }


            ingame.uiContainer.forEach { it.touchUp(screenX, screenY, pointer, button) } // for MouseControlled UIcanvases
        }

        return true
    }

    override fun scrolled(amount: Int): Boolean {
        ingame.uiContainer.forEach { it.scrolled(amount) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        ingame.uiContainer.forEach { it.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        ingame.uiContainer.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

}
