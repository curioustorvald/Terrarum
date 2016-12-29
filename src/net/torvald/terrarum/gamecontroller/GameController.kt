package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.gameactors.Controllable
import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarum.mapdrawer.TilesDrawer
import net.torvald.terrarum.mapdrawer.FeaturesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ProjectileSimple
import net.torvald.terrarum.gameactors.floorInt
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
    internal val mouseX: Float
        get() = (MapCamera.x + Terrarum.appgc.input.mouseX / Terrarum.ingame.screenZoom)
    /** position of the mouse (pixelwise) relative to the world (also, currently pointing world-wise coordinate, if the world coordinate is pixel-wise)*/
    internal val mouseY: Float
        get() = (MapCamera.y + Terrarum.appgc.input.mouseY / Terrarum.ingame.screenZoom)
    /** currently pointing tile coordinate */
    internal val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    internal val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()

    fun processInput(gc: GameContainer, delta: Int, input: Input) {

        KeyToggler.update(input)


        if (!Terrarum.ingame.consoleHandler.isTakingControl) {
            if (Terrarum.ingame.player is Player && (Terrarum.ingame.player as Player).vehicleRiding != null) {
                (Terrarum.ingame.player as Player).vehicleRiding!!.processInput(gc, delta, input)
            }

            Terrarum.ingame.actorContainer.forEach {
                if (it is Controllable) it.processInput(gc, delta, input)
            }

            Terrarum.ingame.uiContainer.forEach {
                it.processInput(gc, delta, input)
            }
        }
        else {
            Terrarum.ingame.consoleHandler.processInput(gc, delta, input)
        }


        ///////////////////
        // MOUSE CONTROL //
        ///////////////////
        // PRIMARY/SECONDARY IS FIXED TO LEFT/RIGHT BUTTON //






        /////////////////////
        // GAMEPAD CONTROL //
        /////////////////////
    }

    fun keyPressed(key: Int, c: Char) {
        if (keyPressedByCode(key, EnumKeyFunc.UI_CONSOLE)) {
            Terrarum.ingame.consoleHandler.toggleOpening()
        }
        else if (keyPressedByCode(key, EnumKeyFunc.UI_BASIC_INFO)) {
            Terrarum.ingame.debugWindow.toggleOpening()
        }



        if (!Terrarum.ingame.consoleHandler.isTakingControl) {
            if (Terrarum.ingame.player is Player && (Terrarum.ingame.player as Player).vehicleRiding != null) {
                (Terrarum.ingame.player as Player).vehicleRiding!!.keyPressed(key, c)
            }

            Terrarum.ingame.player.keyPressed(key, c)
        }
        else {
            Terrarum.ingame.consoleHandler.keyPressed(key, c)
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
        // bullet test
        /*if (button == 0) {
            Terrarum.ingame.addActor(ProjectileSimple(
                    0,
                    Terrarum.ingame.player.centrePosition,
                    Vector2(mouseX.toDouble(), mouseY.toDouble())
            ))
        }*/
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
