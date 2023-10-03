package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.ui.UIQuickslotBar
import net.torvald.terrarum.worlddrawer.WorldCamera
import org.dyn4j.geometry.Vector2
import java.util.*

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
        get() = (mouseX / TILE_SIZE).floorToInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / TILE_SIZE).floorToInt()

    init {
        try {
            if (Controllers.getControllers().size == 0) {
                printdbg(this, "Controller not found")
            }
        }
        catch (e: GdxRuntimeException) {
            printdbgerr(this, "Error initialising controller: ${e.message}")
            e.stackTrace.forEach { printdbgerr(this, "\t$it") }
        }
    }

    private var worldPrimaryClickLatched = false

    private val keyStatus = BitSet(256)
    private var inputMouseX = -1
    private var inputMouseY = -1
    private val mouseStatus = BitSet(8)
    private val controllerButtonStatus = BitSet(64)

    private fun updateKeyboard() {
        /////////////////////
        // GAMEPAD CONTROL //
        /////////////////////


        //////////////////////
        // KEYBOARD CONTROL //
        //////////////////////

        //KeyToggler.update(terrarumIngame.canPlayerControl)
        //printdbg(this, terrarumIngame.canPlayerControl)

        // control key events
        var noKeyHeldDown = true
        for (key in 1..Input.Keys.MAX_KEYCODE) {
            val keyDown = Gdx.input.isKeyPressed(key)

            noKeyHeldDown = noKeyHeldDown and keyDown

            if (keyDown && !keyStatus[key])
                tKeyDown(key)
            else if (!keyDown && keyStatus[key])
                tKeyUp(key)

            keyStatus[key] = keyDown
        }
        // control mouse/touch events
        val newmx = Gdx.input.x
        val newmy = Gdx.input.y
        for (touch in 0..4) {
            val touchDown = Gdx.input.isButtonPressed(touch)

            if (touchDown && !mouseStatus[touch])
                tTouchDown(newmx, newmy, 0, touch)
            else if (!touchDown && mouseStatus[touch])
                tTouchUp(newmx, newmy, 0, touch)

            if (touchDown && mouseStatus.bitCount() != 0) {
                tTouchDragged(newmx, newmy, 0)
            }

            mouseStatus[touch] = touchDown
        }

        inputMouseX = newmx
        inputMouseY = newmy
    }

    private val inventoryCategoryAllowClickAndDrag = listOf(
        GameItem.Category.TOOL,
        GameItem.Category.WALL,
        GameItem.Category.WIRE,
        GameItem.Category.BLOCK,
        null // empty hands won't prevent dragging for barehand action
    )

    fun update() {

        ///////////////////
        // MOUSE CONTROL //
        ///////////////////

        // Use item: assuming the player has only one effective grip (EquipPosition.HAND_GRIP)
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (!terrarumIngame.paused) {
            val actor = terrarumIngame.actorNowPlaying
            val itemOnGrip = terrarumIngame.actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event

            // DON'T DO UI-FILTERING HERE; they're already done on ingame.worldPrimaryClickStart
            // also, some UIs should NOT affect item usage (e.g. quickslot) and ingame's uiOpened property is doing
            // the very job.

            if (actor != null && Terrarum.mouseDown && !worldPrimaryClickLatched) {
                terrarumIngame.worldPrimaryClickStart(actor, App.UPDATE_RATE)
                worldPrimaryClickLatched = true
            }
            if (actor != null && Gdx.input.isButtonPressed(App.getConfigInt("config_mousesecondary"))) {
                terrarumIngame.worldSecondaryClickStart(actor, App.UPDATE_RATE)
            }

            // unlatch when:
            // - not clicking anymore
            // - using any item that is not fixture (blocks, picks)
            // DON'T unlatch when:
            // - performing barehand action
            if (!Terrarum.mouseDown || inventoryCategoryAllowClickAndDrag.contains(ItemCodex[itemOnGrip]?.inventoryCategory)) {
                worldPrimaryClickLatched = false
            }

        }


        updateKeyboard()
    }

    private var f12Down = false

    private fun tKeyDown(keycode: Int): Boolean {

        if (!terrarumIngame.paused) {
            terrarumIngame.actorNowPlaying?.keyDown(keycode)

            // quickslot by number keys
            val quickslotKeys = App.getConfigIntArray("control_key_quickslots")
            if (keycode in quickslotKeys) {
                terrarumIngame.actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, quickslotKeys.indexOf(keycode))
            }

            // pie menu
            if (App.getConfigIntArray("control_key_quickselalt").contains(keycode)
                || keycode == ControlPresets.getKey("control_key_quicksel")) {
                terrarumIngame.uiPieMenu.setAsOpen()
                terrarumIngame.uiQuickBar.setAsClose()
            }

            // toss items
            if (Gdx.input.isKeyJustPressed(ControlPresets.getKey("control_key_discard"))) {
                val player = terrarumIngame.actorNowPlaying
                val item = if (player != null) player.inventory.quickSlot[player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)!!] else null
                if (player != null && item != null) {
                    // remove an item from the inventory
                    player.inventory.remove(item, 1)
                    // create and spawn the droppeditem
                    DroppedItem(item,
                        player.hitbox.centeredX,
                        player.hitbox.centeredY,
                        Vector2(-4.0 * player.scale.sqrt() * player.sprite!!.flipHorizontal.toInt(1).minus(1), -0.1)
                    ).let { drop ->
                        INGAME.queueActorAddition(drop)
                    }
                }
            }
        }

        terrarumIngame.uiContainer.forEach { it?.keyDown(keycode) } // for KeyboardControlled UIcanvases
        
        // Debug UIs
        if (keycode == Input.Keys.GRAVE) {
            terrarumIngame.consoleHandler.toggleOpening()
        }


        // screenshot key
        if (keycode == Input.Keys.F12 && !f12Down) {
            App.requestScreenshot()
            f12Down = true
            println("Screenshot taken.")
        }

        return true
    }

    private fun tKeyUp(keycode: Int): Boolean {
        if (App.getConfigIntArray("control_key_quickselalt").contains(keycode)
            || keycode == ControlPresets.getKey("control_key_quicksel")) {
            terrarumIngame.uiPieMenu.setAsClose()
            terrarumIngame.uiQuickBar.setAsOpen()
        }

        terrarumIngame.uiContainer.forEach { it?.keyUp(keycode) } // for KeyboardControlled UIcanvases
        
        // screenshot key
        if (keycode == Input.Keys.F12) f12Down = false


        return true
    }

    override fun keyTyped(character: Char): Boolean {
        terrarumIngame.uiContainer.forEach { if (it?.isVisible == true) it.keyTyped(character) }
        return true
    }

    private fun tTouchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // don't separate Player from this! Physics will break, esp. airborne manoeuvre
        if (!terrarumIngame.paused) {
            // fire world click events; the event is defined as Ingame's (or any others') WorldClick event
            if (terrarumIngame.uiContainer.map { if ((it?.isOpening == true || it?.isOpened == true) && it.mouseUp) 1 else 0 }.sum() == 0) { // no UI on the mouse, right?

                if (
                        terrarumIngame.actorNowPlaying != null &&
                        (button == App.getConfigInt("config_mouseprimary") ||
                        button == App.getConfigInt("config_mousesecondary"))) {
                    terrarumIngame.worldPrimaryClickEnd(terrarumIngame.actorNowPlaying!!, App.UPDATE_RATE)
                }
                if (button == App.getConfigInt("config_mousesecondary")) {
                    terrarumIngame.worldSecondaryClickEnd(terrarumIngame.actorNowPlaying!!, App.UPDATE_RATE)
                }
            }
        }

        // pie menu
        if (button == App.getConfigInt("control_mouse_quicksel")) {
            terrarumIngame.uiPieMenu.setAsClose()
            terrarumIngame.uiQuickBar.setAsOpen()
        }

        terrarumIngame.uiContainer.forEach { it?.touchUp(screenX, screenY, pointer, button) } // for MouseControlled UIcanvases
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (!terrarumIngame.paused) {
            // quickslot by wheel
            terrarumIngame.actorNowPlaying?.let {
                var selection = it.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)!!

                if (amountX <= -1 || amountY <= -1)
                    selection -= 1
                else if (amountX >= 1 || amountY >= 1)
                    selection += 1

                it.actorValue.set(AVKey.__PLAYER_QUICKSLOTSEL, selection fmod UIQuickslotBar.SLOT_COUNT)
            }
        }
        terrarumIngame.uiContainer.forEach { it?.scrolled(amountX, amountY) }
        return true
    }

    private fun tTouchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        terrarumIngame.uiContainer.forEach { it?.touchDragged(screenX, screenY, pointer) }
        return true
    }

    private fun tTouchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        terrarumIngame.uiContainer.forEach { it?.touchDown(screenX, screenY, pointer, button) }
        
        // pie menu
        if (button == App.getConfigInt("control_mouse_quicksel")) {
            terrarumIngame.uiPieMenu.setAsOpen()
            terrarumIngame.uiQuickBar.setAsClose()
        }

        return true
    }

    private inline fun BitSet.bitCount() = this.cardinality()
}