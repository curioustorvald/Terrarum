package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.mapdrawer.TilesDrawer
import net.torvald.terrarum.mapdrawer.FeaturesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TileCodex
import net.torvald.terrarum.ui.UIHandler
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
object GameController {

    // these four values can also be accessed with GameContainer.<varname>
    // e.g.  gc.mouseTileX

    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise) */
    val mouseX: Float
        get() = (MapCamera.x + Terrarum.appgc.input.mouseX / (Terrarum.ingame?.screenZoom ?: 1f))
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    val mouseY: Float
        get() = (MapCamera.y + Terrarum.appgc.input.mouseY / (Terrarum.ingame?.screenZoom ?: 1f))
    /** currently pointing tile coordinate */
    val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()

    fun processInput(gc: GameContainer, delta: Int, input: Input) {
        if (Terrarum.ingame != null) {
            val ingame = Terrarum.ingame!!


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
                    val itemOnGrip = ingame.player!!.inventory.itemEquipped[InventoryItem.EquipPosition.HAND_GRIP]

                    if (itemOnGrip != null) {
                        if (input.isMouseButtonDown(Terrarum.getConfigInt("mouseprimary"))) {
                            ingame.player!!.consumePrimary(itemOnGrip)
                        }
                        else if (input.isMouseButtonDown(Terrarum.getConfigInt("mousesecondary"))) {
                            ingame.player!!.consumeSecondary(itemOnGrip)
                        }
                    }
                }
            }


            /////////////////////
            // GAMEPAD CONTROL //
            /////////////////////
        }
    }

    fun keyPressed(key: Int, c: Char) {

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
