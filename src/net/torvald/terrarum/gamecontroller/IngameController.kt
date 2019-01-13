package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 2015-12-31.
 */
class IngameController(val ingame: Ingame) : InputAdapter() {


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

    fun update(delta: Float) {

        ///////////////////
        // MOUSE CONTROL //
        ///////////////////

        // Use item: assuming the player has only one effective grip (EquipPosition.HAND_GRIP)
        if (ingame.canPlayerControl) {
            if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary")) ||
                Gdx.input.isButtonPressed(AppLoader.getConfigInt("mousesecondary"))) {

                val player = (Terrarum.ingame!! as Ingame).actorNowPlaying
                if (player == null) return

                val itemOnGrip = player.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                itemOnGrip?.let {
                    if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary"))) {
                        player.consumePrimary(it)
                    }
                    if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mousesecondary"))) {
                        player.consumeSecondary(it)
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
            ingame.actorNowPlaying?.keyDown(keycode)

            // quickslot by number keys
            val quickslotKeys = AppLoader.getConfigIntArray("keyquickslots")
            if (keycode in quickslotKeys) {
                ingame.actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, quickslotKeys.indexOf(keycode))
            }

            // pie menu
            if (AppLoader.getConfigIntArray("keyquickselalt").contains(keycode)
                || keycode == AppLoader.getConfigInt("keyquicksel")) {
                ingame.uiPieMenu.setAsOpen()
                ingame.uiQuickBar.setAsClose()
            }
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
        if (AppLoader.getConfigIntArray("keyquickselalt").contains(keycode)
            || keycode == AppLoader.getConfigInt("keyquicksel")) {
            ingame.uiPieMenu.setAsClose()
            ingame.uiQuickBar.setAsOpen()
        }

        ingame.uiContainer.forEach { it.keyUp(keycode) } // for KeyboardControlled UIcanvases


        return true
    }

    override fun keyTyped(character: Char): Boolean {
        ingame.uiContainer.forEach { if (it.isVisible) it.keyTyped(character) }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        ingame.uiContainer.forEach { it.mouseMoved(screenX, screenY) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (ingame.canPlayerControl) {
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event
            if (ingame.uiContainer.map { if ((it.isOpening || it.isOpened) && it.mouseUp) 1 else 0 }.sum() == 0) { // no UI on the mouse, right?

                if (button == AppLoader.getConfigInt("mouseprimary")) {
                    ingame.worldPrimaryClickEnd(Terrarum.deltaTime)
                }
                if (button == AppLoader.getConfigInt("mousesecondary")) {
                    ingame.worldSecondaryClickEnd(Terrarum.deltaTime)
                }
            }
        }


        ingame.uiContainer.forEach { it.touchUp(screenX, screenY, pointer, button) } // for MouseControlled UIcanvases

        return true
    }

    override fun scrolled(amount: Int): Boolean {
        if (ingame.canPlayerControl) {
            // quickslot by wheel
            if (ingame.actorNowPlaying != null) {
                ingame.actorNowPlaying!!.actorValue.set(
                        AVKey.__PLAYER_QUICKSLOTSEL,
                        (ingame.actorNowPlaying!!.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)!! - amount) fmod ingame.actorNowPlaying!!.inventory.quickSlot.size
                )
            }
        }

        ingame.uiContainer.forEach { it.scrolled(amount) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        ingame.uiContainer.forEach { it.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (ingame.canPlayerControl) {
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event
            if (ingame.uiContainer.map { if ((it.isOpening || it.isOpened) && it.mouseUp) 1 else 0 }.sum() == 0) { // no UI on the mouse, right?

                if (button == AppLoader.getConfigInt("mouseprimary")) {
                    ingame.worldPrimaryClickStart(Terrarum.deltaTime)
                }
                if (button == AppLoader.getConfigInt("mousesecondary")) {
                    ingame.worldSecondaryClickStart(Terrarum.deltaTime)
                }
            }
        }


        ingame.uiContainer.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

}
