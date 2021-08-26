package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printdbgerr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.worlddrawer.WorldCamera
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
        get() = (mouseX / TILE_SIZE).floorInt()
    /** currently pointing tile coordinate */
    val mouseTileY: Int
        get() = (mouseY / TILE_SIZE).floorInt()

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
            else if (!touchDown && keyStatus[touch])
                tTouchUp(newmx, newmy, 0, touch)

            if (touchDown && mouseStatus.bitCount() != 0) {
                tTouchDragged(newmx, newmy, 0)
            }

            mouseStatus[touch] = touchDown
        }

        inputMouseX = newmx
        inputMouseY = newmy
    }

    fun update() {

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

            if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mouseprimary")) && !worldPrimaryClickLatched) {
                terrarumIngame.worldPrimaryClickStart(AppLoader.UPDATE_RATE)
                worldPrimaryClickLatched = true
            }
            /*if Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mousesecondary")) {
                ingame.worldSecondaryClickStart(AppLoader.UPDATE_RATE)
            }*/

            // unlatch when:
            // - not clicking anymore
            // - using any item that is not fixture (blocks, picks)
            if (!Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mouseprimary")) ||
                GameItem.Category.FIXTURE != ItemCodex.get(terrarumIngame.actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP))?.inventoryCategory) {
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
            val quickslotKeys = AppLoader.getConfigIntArray("config_keyquickslots")
            if (keycode in quickslotKeys) {
                terrarumIngame.actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, quickslotKeys.indexOf(keycode))
            }

            // pie menu
            if (AppLoader.getConfigIntArray("config_keyquickselalt").contains(keycode)
                || keycode == AppLoader.getConfigInt("config_keyquicksel")) {
                terrarumIngame.uiPieMenu.setAsOpen()
                terrarumIngame.uiQuickBar.setAsClose()
            }
        }

        terrarumIngame.uiContainer.forEach { it?.keyDown(keycode) } // for KeyboardControlled UIcanvases
        
        // Debug UIs
        if (keycode == Input.Keys.GRAVE) {
            terrarumIngame.consoleHandler.toggleOpening()
        }


        // screenshot key
        if (keycode == Input.Keys.F12 && !f12Down) {
            AppLoader.requestScreenshot()
            f12Down = true
            println("Screenshot taken.")
        }

        return true
    }

    private fun tKeyUp(keycode: Int): Boolean {
        if (AppLoader.getConfigIntArray("config_keyquickselalt").contains(keycode)
            || keycode == AppLoader.getConfigInt("config_keyquicksel")) {
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
                        button == AppLoader.getConfigInt("config_mouseprimary") ||
                        button == AppLoader.getConfigInt("config_mousesecondary")) {
                    terrarumIngame.worldPrimaryClickEnd(AppLoader.UPDATE_RATE)
                }
                /*if (button == AppLoader.getConfigInt("config_mousesecondary")) {
                    ingame.worldSecondaryClickEnd(AppLoader.UPDATE_RATE)
                }*/
            }
        }

        // pie menu
        if (button == AppLoader.getConfigInt("config_mousequicksel")) {
            terrarumIngame.uiPieMenu.setAsClose()
            terrarumIngame.uiQuickBar.setAsOpen()
        }

        terrarumIngame.uiContainer.forEach { it?.touchUp(screenX, screenY, pointer, button) } // for MouseControlled UIcanvases
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (!terrarumIngame.paused) {
            // quickslot by wheel
            if (terrarumIngame.actorNowPlaying != null) {
                terrarumIngame.actorNowPlaying!!.actorValue.set(
                        AVKey.__PLAYER_QUICKSLOTSEL,
                        (terrarumIngame.actorNowPlaying!!.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)!! - amountY.toInt()) fmod terrarumIngame.actorNowPlaying!!.inventory.quickSlot.size
                )
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
        if (button == AppLoader.getConfigInt("config_mousequicksel")) {
            terrarumIngame.uiPieMenu.setAsOpen()
            terrarumIngame.uiQuickBar.setAsClose()
        }

        return true
    }

    companion object {
        const val KEY_DELAY = 0.2f
        const val KEY_REPEAT = 1f / 40f
        val KEYCODE_TO_CHAR = hashMapOf<Int, Char>(
                Keys.NUM_1 to '1',
                Keys.NUM_2 to '2',
                Keys.NUM_3 to '3',
                Keys.NUM_4 to '4',
                Keys.NUM_5 to '5',
                Keys.NUM_6 to '6',
                Keys.NUM_7 to '7',
                Keys.NUM_8 to '8',
                Keys.NUM_9 to '9',
                Keys.NUM_0 to '0',

                Keys.A to 'a',
                Keys.B to 'b',
                Keys.C to 'c',
                Keys.D to 'd',
                Keys.E to 'e',
                Keys.F to 'f',
                Keys.G to 'g',
                Keys.H to 'h',
                Keys.I to 'i',
                Keys.J to 'j',
                Keys.K to 'k',
                Keys.L to 'l',
                Keys.M to 'm',
                Keys.N to 'n',
                Keys.O to 'o',
                Keys.P to 'p',
                Keys.Q to 'q',
                Keys.R to 'r',
                Keys.S to 's',
                Keys.T to 't',
                Keys.U to 'u',
                Keys.V to 'v',
                Keys.W to 'w',
                Keys.X to 'x',
                Keys.Y to 'y',
                Keys.Z to 'z',

                Keys.GRAVE to '`',
                Keys.MINUS to '-',
                Keys.EQUALS to '=',
                Keys.BACKSPACE to 8.toChar(),

                Keys.LEFT_BRACKET to '[',
                Keys.RIGHT_BRACKET to ']',
                Keys.BACKSLASH to '\\',

                Keys.SEMICOLON to ';',
                Keys.APOSTROPHE to '\'',
                Keys.ENTER to 10.toChar(),

                Keys.COMMA to ',',
                Keys.PERIOD to '.',
                Keys.SLASH to '/',

                Keys.SPACE to ' ',

                Keys.NUMPAD_0 to '0',
                Keys.NUMPAD_1 to '1',
                Keys.NUMPAD_2 to '2',
                Keys.NUMPAD_3 to '3',
                Keys.NUMPAD_4 to '4',
                Keys.NUMPAD_5 to '5',
                Keys.NUMPAD_6 to '6',
                Keys.NUMPAD_7 to '7',
                Keys.NUMPAD_8 to '8',
                Keys.NUMPAD_9 to '9',

                Keys.NUMPAD_DIVIDE to '/',
                Keys.NUMPAD_MULTIPLY to '*',
                Keys.NUMPAD_SUBTRACT to '-',
                Keys.NUMPAD_ADD to '+',
                Keys.NUMPAD_DOT to '.',
                Keys.NUMPAD_ENTER to 10.toChar()
        )
        val KEYCODE_TO_CHAR_SHIFT = hashMapOf<Int, Char>(
                Keys.NUM_1 to '!',
                Keys.NUM_2 to '@',
                Keys.NUM_3 to '#',
                Keys.NUM_4 to '$',
                Keys.NUM_5 to '%',
                Keys.NUM_6 to '^',
                Keys.NUM_7 to '&',
                Keys.NUM_8 to '*',
                Keys.NUM_9 to '(',
                Keys.NUM_0 to ')',

                Keys.A to 'A',
                Keys.B to 'B',
                Keys.C to 'C',
                Keys.D to 'D',
                Keys.E to 'E',
                Keys.F to 'F',
                Keys.G to 'G',
                Keys.H to 'H',
                Keys.I to 'I',
                Keys.J to 'J',
                Keys.K to 'K',
                Keys.L to 'L',
                Keys.M to 'M',
                Keys.N to 'N',
                Keys.O to 'O',
                Keys.P to 'P',
                Keys.Q to 'Q',
                Keys.R to 'R',
                Keys.S to 'S',
                Keys.T to 'T',
                Keys.U to 'U',
                Keys.V to 'V',
                Keys.W to 'W',
                Keys.X to 'X',
                Keys.Y to 'Y',
                Keys.Z to 'Z',

                Keys.GRAVE to '~',
                Keys.MINUS to '_',
                Keys.EQUALS to '+',
                Keys.BACKSPACE to 8.toChar(),

                Keys.LEFT_BRACKET to '{',
                Keys.RIGHT_BRACKET to '}',
                Keys.BACKSLASH to '|',

                Keys.SEMICOLON to ':',
                Keys.APOSTROPHE to '"',
                Keys.ENTER to 10.toChar(),

                Keys.COMMA to '<',
                Keys.PERIOD to '>',
                Keys.SLASH to '?',

                Keys.SPACE to ' ',

                Keys.NUMPAD_0 to '0',
                Keys.NUMPAD_1 to '1',
                Keys.NUMPAD_2 to '2',
                Keys.NUMPAD_3 to '3',
                Keys.NUMPAD_4 to '4',
                Keys.NUMPAD_5 to '5',
                Keys.NUMPAD_6 to '6',
                Keys.NUMPAD_7 to '7',
                Keys.NUMPAD_8 to '8',
                Keys.NUMPAD_9 to '9',

                Keys.NUMPAD_DIVIDE to '/',
                Keys.NUMPAD_MULTIPLY to '*',
                Keys.NUMPAD_SUBTRACT to '-',
                Keys.NUMPAD_ADD to '+',
                Keys.NUMPAD_DOT to '.',
                Keys.NUMPAD_ENTER to 10.toChar()
        )
    }

    private inline fun BitSet.bitCount() = this.cardinality()

}