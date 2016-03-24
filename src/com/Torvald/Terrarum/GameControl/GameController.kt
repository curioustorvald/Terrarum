package com.Torvald.Terrarum.GameControl

import com.Torvald.Terrarum.Actors.Controllable
import com.Torvald.Terrarum.Actors.Player
import com.Torvald.Terrarum.MapDrawer.MapCamera
import com.Torvald.Terrarum.MapDrawer.MapDrawer
import com.Torvald.Terrarum.Terrarum
import com.Torvald.Terrarum.TileProperties.TileNameCode
import com.Torvald.Terrarum.TileProperties.TilePropCodex
import com.Torvald.Terrarum.UserInterface.UIHandler
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
object GameController {

    fun processInput(input: Input) {
        val mouseTileX = ((MapCamera.cameraX + input.mouseX / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.cameraY + input.mouseY / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()


        KeyToggler.update(input)


        if (!Terrarum.game.consoleHandler.isTakingControl) {
            if (Terrarum.game.player.vehicleRiding != null) {
                Terrarum.game.player.vehicleRiding!!.processInput(input)
            }

            Terrarum.game.player.processInput(input)

            for (ui in Terrarum.game.uiContainer) {
                ui.processInput(input)
            }
        }
        else {
            Terrarum.game.consoleHandler.processInput(input)
        }


        if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON)) {
            // test tile remove
            try {
                Terrarum.game.map.setTileTerrain(mouseTileX, mouseTileY, TileNameCode.AIR)
                // Terrarum.game.map.setTileWall(mouseTileX, mouseTileY, TileNameCode.AIR);
            }
            catch (e: ArrayIndexOutOfBoundsException) {
            }

        }
        else if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) {
            // test tile place
            try {
                Terrarum.game.map.setTileTerrain(mouseTileX, mouseTileY, Terrarum.game.player.getActorValue().getAsInt("selectedtile")!!)
            }
            catch (e: ArrayIndexOutOfBoundsException) {
            }

        }
    }

    fun keyPressed(key: Int, c: Char) {
        if (keyPressedByCode(key, EnumKeyFunc.UI_CONSOLE)) {
            Terrarum.game.consoleHandler.toggleOpening()
        }
        else if (keyPressedByCode(key, EnumKeyFunc.UI_BASIC_INFO)) {
            Terrarum.game.debugWindow.toggleOpening()
        }



        if (!Terrarum.game.consoleHandler.isTakingControl) {
            if (Terrarum.game.player.vehicleRiding != null) {
                Terrarum.game.player.vehicleRiding!!.keyPressed(key, c)
            }

            Terrarum.game.player.keyPressed(key, c)
        }
        else {
            Terrarum.game.consoleHandler.keyPressed(key, c)
        }

        //System.out.println(String.valueOf(key) + ", " + String.valueOf(c));
    }

    fun keyReleased(key: Int, c: Char) {

    }

    fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {

    }

    fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {

    }

    fun mousePressed(button: Int, x: Int, y: Int) {

    }

    fun mouseReleased(button: Int, x: Int, y: Int) {

    }

    fun mouseWheelMoved(change: Int) {

    }

    fun controllerButtonPressed(controller: Int, button: Int) {

    }

    fun controllerButtonReleased(controller: Int, button: Int) {

    }

    private fun keyPressedByCode(key: Int, fn: EnumKeyFunc): Boolean {
        return KeyMap.getKeyCode(fn) == key
    }
}
