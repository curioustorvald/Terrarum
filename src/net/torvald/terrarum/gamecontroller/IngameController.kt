package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameworld.fmod
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

            if (terrarumIngame.actorNowPlaying != null && Terrarum.mouseDown && !worldPrimaryClickLatched) {
                terrarumIngame.worldPrimaryClickStart(terrarumIngame.actorNowPlaying!!, App.UPDATE_RATE)
                worldPrimaryClickLatched = true
            }
            /*if Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mousesecondary")) {
                ingame.worldSecondaryClickStart(AppLoader.UPDATE_RATE)
            }*/

            // unlatch when:
            // - not clicking anymore
            // - using any item that is not fixture (blocks, picks)
            if (!Terrarum.mouseDown ||
                GameItem.Category.MISC != ItemCodex.get(terrarumIngame.actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP))?.inventoryCategory) {
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
                || keycode == App.getConfigInt("control_key_quicksel")) {
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
            App.requestScreenshot()
            f12Down = true
            println("Screenshot taken.")
        }

        return true
    }

    private fun tKeyUp(keycode: Int): Boolean {
        if (App.getConfigIntArray("control_key_quickselalt").contains(keycode)
            || keycode == App.getConfigInt("control_key_quicksel")) {
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
                /*if (button == AppLoader.getConfigInt("config_mousesecondary")) {
                    ingame.worldSecondaryClickEnd(AppLoader.UPDATE_RATE)
                }*/
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
        if (button == App.getConfigInt("control_mouse_quicksel")) {
            terrarumIngame.uiPieMenu.setAsOpen()
            terrarumIngame.uiQuickBar.setAsClose()
        }

        return true
    }

    companion object {
        data class TerrarumKeyboardEvent(
                val type: Int,
                val character: String?, // representative key symbol
                val headkey: Int, // representative keycode
                val repeatCount: Int,
                val keycodes: IntArray
        )
        private const val KEY_DOWN = 0
        private const val KEY_CHANGE = 1
        const val N_KEY_ROLLOVER = 8
        var KEYBOARD_DELAYS = longArrayOf(0L,250000000L,0L,25000000L,0L)
        private var stroboTime = 0L
        private var stroboStatus = 0
        private var repeatCount = 0
        private var oldKeys = IntArray(N_KEY_ROLLOVER) { 0 }
        /** always Low Layer */
//        private var keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

        fun resetKeyboardStrobo() {
            stroboStatus = 0
            repeatCount = 0
        }

        // code proudly stolen from tsvm's TVDOS.SYS
        fun withKeyboardEvent(callback: (TerrarumKeyboardEvent) -> Unit) {
            val keys = strobeKeys()
            var keyChanged = !arrayEq(keys, oldKeys)
            val keyDiff = arrayDiff(keys, oldKeys)
            val keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

            if (stroboStatus % 2 == 0 && keys[0] != 0) {
                stroboStatus += 1
                stroboTime = System.nanoTime()
                repeatCount += 1

                val shiftin = keys.contains(Keys.SHIFT_LEFT) || keys.contains(Keys.SHIFT_RIGHT)
                val keysym0 = keysToStr(keymap, keys)
                val newKeysym0 = keysToStr(keymap, keyDiff)
                val keysym = if (keysym0 == null) null
                else if (shiftin && keysym0[1]?.isNotBlank() == true) keysym0[1]
                else keysym0[0]
                val newKeysym = if (newKeysym0 == null) null
                else if (shiftin && newKeysym0[1]?.isNotBlank() == true) newKeysym0[1]
                else newKeysym0[0]

                val headKeyCode = if (keyDiff.size < 1) keys[0] else keyDiff[0]

                if (!keyChanged) {
//                    println("KEY_DOWN '$keysym' ($headKeyCode) $repeatCount; ${keys.joinToString()}")
                    callback(TerrarumKeyboardEvent(KEY_DOWN, keysym, headKeyCode, repeatCount, keys))
                }
                else if (newKeysym != null) {
//                    println("KEY_DOWC '$newKeysym' ($headKeyCode) $repeatCount; ${keys.joinToString()}")
                    callback(TerrarumKeyboardEvent(KEY_DOWN, newKeysym, headKeyCode, repeatCount, keys))
                }

                oldKeys = keys // don't put this outside of if-cascade
            }
            else if (keyChanged || keys[0] == 0) {
                stroboStatus = 0
                repeatCount = 0

                if (keys[0] == 0) keyChanged = false
            }
            else if (stroboStatus % 2 == 1 && System.nanoTime() - stroboTime < KEYBOARD_DELAYS[stroboStatus]) {
                Thread.sleep(1L)
            }
            else {
                stroboStatus += 1
                if (stroboStatus >= 4)
                    stroboStatus = 2
            }
        }

        private fun keysToStr(keymap: TerrarumKeyLayout, keys: IntArray): Array<String?>? {
            if (keys.size == 0) return null
            val headkey = keys[0]
            return keymap.symbols?.get(headkey)
        }

        private fun strobeKeys(): IntArray {
            var keysPushed = 0
            val keyEventBuffers = IntArray(N_KEY_ROLLOVER) { 0 }
            for (k in 1..254) {
                if (Gdx.input.isKeyPressed(k)) {
                    keyEventBuffers[keysPushed] = k
                    keysPushed += 1
                }

                if (keysPushed >= N_KEY_ROLLOVER) break
            }
            return keyEventBuffers
        }

        private fun arrayEq(a: IntArray, b: IntArray): Boolean {
            for (i in 0 until a.size) {
                if (a[i] != b.getOrNull(i)) return false
            }
            return true
        }

        private fun arrayDiff(a: IntArray, b: IntArray): IntArray {
            return a.filter { !b.contains(it) }.toIntArray()
        }
    }

    private inline fun BitSet.bitCount() = this.cardinality()
}