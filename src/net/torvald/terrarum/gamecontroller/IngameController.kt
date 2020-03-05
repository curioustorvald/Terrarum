package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controllers
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 2015-12-31.
 */
class IngameController(val terrarumIngame: TerrarumIngame) : InputAdapter() {


    val hasGamepad: Boolean
        get() = gamepad != null
    var gamepad: TerrarumController? = null

    // these four values can also be accessed with GameContainer.<varname>
    // e.g.  gc.mouseTileX

    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
    val mouseX: Float
        get() = WorldCamera.x + Gdx.input.x / (terrarumIngame.screenZoom)
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    val mouseY: Float
        get() = WorldCamera.y + Gdx.input.y / (terrarumIngame.screenZoom)
    /** currently pointing tile coordinate */
    val mouseTileX: Int
        get() = (mouseX / CreateTileAtlas.TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / CreateTileAtlas.TILE_SIZE).floorInt()

    init {
        if (Controllers.getControllers().size == 0) {
            printdbg(this, "Controller not found")
        }
    }

    fun update(delta: Float) {

        ///////////////////
        // MOUSE CONTROL //
        ///////////////////

        // Use item: assuming the player has only one effective grip (EquipPosition.HAND_GRIP)
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (!terrarumIngame.paused) {
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event

            // DON'T DO UI-FILTERING HERE; they're already done on ingame.worldPrimaryClickStart
            // also, some UIs should NOT affect item usage (e.g. quickslot) and ingame's uiOpened property is doing
            // the very job.

            if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary"))) {
                terrarumIngame.worldPrimaryClickStart(AppLoader.UPDATE_RATE)
            }
            /*if Gdx.input.isButtonPressed(AppLoader.getConfigInt("mousesecondary")) {
                ingame.worldSecondaryClickStart(AppLoader.UPDATE_RATE)
            }*/

        }


        /////////////////////
        // GAMEPAD CONTROL //
        /////////////////////


        //////////////////////
        // KEYBOARD CONTROL //
        //////////////////////

        //KeyToggler.update(terrarumIngame.canPlayerControl)
        //printdbg(this, terrarumIngame.canPlayerControl)

    }

    private var f12Down = false

    override fun keyDown(keycode: Int): Boolean {

        if (!terrarumIngame.paused) {
            terrarumIngame.actorNowPlaying?.keyDown(keycode)

            // quickslot by number keys
            val quickslotKeys = AppLoader.getConfigIntArray("keyquickslots")
            if (keycode in quickslotKeys) {
                terrarumIngame.actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, quickslotKeys.indexOf(keycode))
            }

            // pie menu
            if (AppLoader.getConfigIntArray("keyquickselalt").contains(keycode)
                || keycode == AppLoader.getConfigInt("keyquicksel")) {
                terrarumIngame.uiPieMenu.setAsOpen()
                terrarumIngame.uiQuickBar.setAsClose()
            }
        }

        terrarumIngame.uiContainer.forEach { it.keyDown(keycode) } // for KeyboardControlled UIcanvases

        // Debug UIs
        if (keycode == Input.Keys.GRAVE) {
            terrarumIngame.consoleHandler.toggleOpening()
        }


        // screenshot key
        if (keycode == Input.Keys.F12 && !f12Down) {
            AppLoader.requestScreenshot()
            terrarumIngame.sendNotification("Screenshot taken")
            f12Down = true
            println("Screenshot taken.")
        }

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        if (AppLoader.getConfigIntArray("keyquickselalt").contains(keycode)
            || keycode == AppLoader.getConfigInt("keyquicksel")) {
            terrarumIngame.uiPieMenu.setAsClose()
            terrarumIngame.uiQuickBar.setAsOpen()
        }

        terrarumIngame.uiContainer.forEach { it.keyUp(keycode) } // for KeyboardControlled UIcanvases


        // screenshot key
        if (keycode == Input.Keys.F12) f12Down = false


        return true
    }

    override fun keyTyped(character: Char): Boolean {
        terrarumIngame.uiContainer.forEach { if (it.isVisible) it.keyTyped(character) }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        terrarumIngame.uiContainer.forEach { it.mouseMoved(screenX, screenY) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (!terrarumIngame.paused) {
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event
            if (terrarumIngame.uiContainer.map { if ((it.isOpening || it.isOpened) && it.mouseUp) 1 else 0 }.sum() == 0) { // no UI on the mouse, right?

                if (
                        button == AppLoader.getConfigInt("mouseprimary") ||
                        button == AppLoader.getConfigInt("mousesecondary")) {
                    terrarumIngame.worldPrimaryClickEnd(AppLoader.UPDATE_RATE)
                }
                /*if (button == AppLoader.getConfigInt("mousesecondary")) {
                    ingame.worldSecondaryClickEnd(AppLoader.UPDATE_RATE)
                }*/
            }
        }

        // pie menu
        if (button == AppLoader.getConfigInt("mousequicksel")) {
            terrarumIngame.uiPieMenu.setAsClose()
            terrarumIngame.uiQuickBar.setAsOpen()
        }

        terrarumIngame.uiContainer.forEach { it.touchUp(screenX, screenY, pointer, button) } // for MouseControlled UIcanvases

        return true
    }

    override fun scrolled(amount: Int): Boolean {
        if (!terrarumIngame.paused) {
            // quickslot by wheel
            if (terrarumIngame.actorNowPlaying != null) {
                terrarumIngame.actorNowPlaying!!.actorValue.set(
                        AVKey.__PLAYER_QUICKSLOTSEL,
                        (terrarumIngame.actorNowPlaying!!.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)!! - amount) fmod terrarumIngame.actorNowPlaying!!.inventory.quickSlot.size
                )
            }
        }

        terrarumIngame.uiContainer.forEach { it.scrolled(amount) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        terrarumIngame.uiContainer.forEach { it.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        terrarumIngame.uiContainer.forEach { it.touchDown(screenX, screenY, pointer, button) }

        // pie menu
        if (button == AppLoader.getConfigInt("mousequicksel")) {
            terrarumIngame.uiPieMenu.setAsOpen()
            terrarumIngame.uiQuickBar.setAsClose()
        }

        return true
    }


}
