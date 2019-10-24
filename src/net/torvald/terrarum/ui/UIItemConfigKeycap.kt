package net.torvald.terrarum.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by Torvald on 2019-10-17.
 */
class UIItemConfigKeycap(
        parent: UICanvas,
        override var posX: Int,
        override var posY: Int,

        private val keySize: Int,

        private val keycode: Int, // also used to draw key label
        private var keyItem: Int? = null  // internal index for the key's behaviour, also used to choose appropriate icon
) : UIItem(parent) {

    init {
        if (keySize < 3) throw IllegalArgumentException("Key size must be greater than 2 (got $keySize)")

        CommonResourcePool.addToLoadingList("ui_item_keymap_keycap") {
            TextureRegionPack("./assets/graphics/gui/ui_config_keymap_keycap.tga", 8, 32)
        }
        CommonResourcePool.loadAll()
    }

    private val capTex = CommonResourcePool.getAsTextureRegionPack("ui_item_keymap_keycap")

    override val width = capTex.tileW * keySize
    override val height = capTex.tileH

    override var oldPosX = posX
    override var oldPosY = posY

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        // draw keycap
        batch.draw(capTex.get(0, 0), posX.toFloat(), posY.toFloat())
        batch.draw(capTex.get(1, 0), (posX + capTex.tileW).toFloat(), posY.toFloat(),
                (capTex.tileW * keySize.minus(2)).toFloat(), height.toFloat()
        )
        batch.draw(capTex.get(2, 0), (posX + (keySize - 1) * capTex.tileW).toFloat(), posY.toFloat())

        // draw label
        TinyAlphNum.draw(batch, KeyDict.keycodeToLabel[keycode]!!, posX + 2f, posY + 2f)

        // draw icon
        

        super.render(batch, camera)
    }

    override fun dispose() {

    }
}


object KeyDict {
    // NOTE: this keydict does NOT have numpad_0..numpad_9
    private val keyDict = arrayOf( // arrayOf(keyID, keycode, keylabel(CP437))
            arrayOf("NUM_0", Input.Keys.NUM_0, "0"), // use toUppercase() if necessary
            arrayOf("NUM_1", Input.Keys.NUM_1, "1"),
            arrayOf("NUM_2", Input.Keys.NUM_2, "2"),
            arrayOf("NUM_3", Input.Keys.NUM_3, "3"),
            arrayOf("NUM_4", Input.Keys.NUM_4, "4"),
            arrayOf("NUM_5", Input.Keys.NUM_5, "5"),
            arrayOf("NUM_6", Input.Keys.NUM_6, "6"),
            arrayOf("NUM_7", Input.Keys.NUM_7, "7"),
            arrayOf("NUM_8", Input.Keys.NUM_8, "8"),
            arrayOf("NUM_9", Input.Keys.NUM_9, "9"),
            arrayOf("A", Input.Keys.A, "A"),
            arrayOf("ALT_LEFT", Input.Keys.ALT_LEFT, "Alt"),
            arrayOf("ALT_RIGHT", Input.Keys.ALT_RIGHT, "Alt"),
            arrayOf("APOSTROPHE", Input.Keys.APOSTROPHE, "'"),
            arrayOf("AT", Input.Keys.AT, "@"),
            arrayOf("B", Input.Keys.B, "B"),
            arrayOf("BACK", Input.Keys.BACK, "Back"),
            arrayOf("BACKSLASH", Input.Keys.BACKSLASH, "\\"),
            arrayOf("C", Input.Keys.C, "C"),
            arrayOf("CALL", Input.Keys.CALL, "Call"),
            arrayOf("CAMERA", Input.Keys.CAMERA, "Cam"),
            arrayOf("CLEAR", Input.Keys.CLEAR, "Clear"),
            arrayOf("COMMA", Input.Keys.COMMA, "Comma"),
            arrayOf("D", Input.Keys.D, "D"),
            arrayOf("DEL", Input.Keys.DEL, "Del"),
            arrayOf("BACKSPACE", Input.Keys.BACKSPACE, "Bksp"),
            arrayOf("FORWARD_DEL", Input.Keys.FORWARD_DEL, "Del"),
            arrayOf("DPAD_CENTER", Input.Keys.DPAD_CENTER, "_"),
            arrayOf("DPAD_DOWN", Input.Keys.DPAD_DOWN, "_"),
            arrayOf("DPAD_LEFT", Input.Keys.DPAD_LEFT, "_"),
            arrayOf("DPAD_RIGHT", Input.Keys.DPAD_RIGHT, "_"),
            arrayOf("DPAD_UP", Input.Keys.DPAD_UP, "_"),
            arrayOf("CENTER", Input.Keys.CENTER, "_"),
            arrayOf("DOWN", Input.Keys.DOWN, "${0x19.toChar()}"),
            arrayOf("LEFT", Input.Keys.LEFT, "${0x1B.toChar()}"),
            arrayOf("RIGHT", Input.Keys.RIGHT, "${0x1A.toChar()}"),
            arrayOf("UP", Input.Keys.UP, "${0x18.toChar()}"),
            arrayOf("E", Input.Keys.E, "E"),
            arrayOf("ENDCALL", Input.Keys.ENDCALL, "EndCall"),
            arrayOf("ENTER", Input.Keys.ENTER, "Enter"),
            arrayOf("ENVELOPE", Input.Keys.ENVELOPE, "Mail"),
            arrayOf("EQUALS", Input.Keys.EQUALS, "="),
            arrayOf("EXPLORER", Input.Keys.EXPLORER, "Expl"),
            arrayOf("F", Input.Keys.F, "F"),
            arrayOf("FOCUS", Input.Keys.FOCUS, "Focus"),
            arrayOf("G", Input.Keys.G, "G"),
            arrayOf("GRAVE", Input.Keys.GRAVE, "`"),
            arrayOf("H", Input.Keys.H, "H"),
            arrayOf("HEADSETHOOK", Input.Keys.HEADSETHOOK, "Headset"),
            arrayOf("HOME", Input.Keys.HOME, "Home"),
            arrayOf("I", Input.Keys.I, "I"),
            arrayOf("J", Input.Keys.J, "J"),
            arrayOf("K", Input.Keys.K, "K"),
            arrayOf("L", Input.Keys.L, "L"),
            arrayOf("LEFT_BRACKET", Input.Keys.LEFT_BRACKET, "["),
            arrayOf("M", Input.Keys.M, "M"),
            arrayOf("MEDIA_FAST_FORWARD", Input.Keys.MEDIA_FAST_FORWARD, "${0x10.toChar()}${0x10.toChar()}"),
            arrayOf("MEDIA_NEXT", Input.Keys.MEDIA_NEXT, "Next"),
            arrayOf("MEDIA_PLAY_PAUSE", Input.Keys.MEDIA_PLAY_PAUSE, "${0x10.toChar()}"),
            arrayOf("MEDIA_PREVIOUS", Input.Keys.MEDIA_PREVIOUS, "Prev"),
            arrayOf("MEDIA_REWIND", Input.Keys.MEDIA_REWIND, "${0x11.toChar()}${0x11.toChar()}"),
            arrayOf("MEDIA_STOP", Input.Keys.MEDIA_STOP, "${0xFE.toChar()}"),
            arrayOf("MENU", Input.Keys.MENU, "${0xF0.toChar()}"),
            arrayOf("MINUS", Input.Keys.MINUS, "-"),
            arrayOf("MUTE", Input.Keys.MUTE, "Mute"),
            arrayOf("N", Input.Keys.N, "M"),
            arrayOf("NOTIFICATION", Input.Keys.NOTIFICATION, "Notif"),
            arrayOf("NUM", Input.Keys.NUM, "Num"),
            arrayOf("O", Input.Keys.O, "O"),
            arrayOf("P", Input.Keys.P, "P"),
            arrayOf("PERIOD", Input.Keys.PERIOD, "."),
            arrayOf("PLUS", Input.Keys.PLUS, "+"),
            arrayOf("POUND", Input.Keys.POUND, "#"),
            arrayOf("POWER", Input.Keys.POWER, "Pwr"),
            arrayOf("Q", Input.Keys.Q, "Q"),
            arrayOf("R", Input.Keys.R, "R"),
            arrayOf("RIGHT_BRACKET", Input.Keys.RIGHT_BRACKET, "]"),
            arrayOf("S", Input.Keys.S, "S"),
            arrayOf("SEARCH", Input.Keys.SEARCH, "Find"),
            arrayOf("SEMICOLON", Input.Keys.SEMICOLON, ";"),
            arrayOf("SHIFT_LEFT", Input.Keys.SHIFT_LEFT, "Shift"),
            arrayOf("SHIFT_RIGHT", Input.Keys.SHIFT_RIGHT, "Shift"),
            arrayOf("SLASH", Input.Keys.SLASH, "/"),
            arrayOf("SOFT_LEFT", Input.Keys.SOFT_LEFT, "S${0x1B.toChar()}"),
            arrayOf("SOFT_RIGHT", Input.Keys.SOFT_RIGHT, "S${0x1A.toChar()}"),
            arrayOf("SPACE", Input.Keys.SPACE, " "),
            arrayOf("STAR", Input.Keys.STAR, "*"),
            arrayOf("SYM", Input.Keys.SYM, "Sym"),
            arrayOf("T", Input.Keys.T, "T"),
            arrayOf("TAB", Input.Keys.TAB, "Tab"),
            arrayOf("U", Input.Keys.U, "U"),
            arrayOf("UNKNOWN", Input.Keys.UNKNOWN, "??"),
            arrayOf("V", Input.Keys.V, "V"),
            arrayOf("VOLUME_DOWN", Input.Keys.VOLUME_DOWN, "V-"),
            arrayOf("VOLUME_UP", Input.Keys.VOLUME_UP, "V+"),
            arrayOf("W", Input.Keys.W, "W"),
            arrayOf("X", Input.Keys.X, "X"),
            arrayOf("Y", Input.Keys.Y, "Y"),
            arrayOf("Z", Input.Keys.Z, "Z"),
            arrayOf("META_ALT_LEFT_ON", Input.Keys.META_ALT_LEFT_ON, "_"),
            arrayOf("META_ALT_ON", Input.Keys.META_ALT_ON, "_"),
            arrayOf("META_ALT_RIGHT_ON", Input.Keys.META_ALT_RIGHT_ON, "_"),
            arrayOf("META_SHIFT_LEFT_ON", Input.Keys.META_SHIFT_LEFT_ON, "_"),
            arrayOf("META_SHIFT_ON", Input.Keys.META_SHIFT_ON, "_"),
            arrayOf("META_SHIFT_RIGHT_ON", Input.Keys.META_SHIFT_RIGHT_ON, "_"),
            arrayOf("META_SYM_ON", Input.Keys.META_SYM_ON, "_"),
            arrayOf("CONTROL_LEFT", Input.Keys.CONTROL_LEFT, "Ctr"),
            arrayOf("CONTROL_RIGHT", Input.Keys.CONTROL_RIGHT, "Ctr"),
            arrayOf("ESCAPE", Input.Keys.ESCAPE, "Esc"),
            arrayOf("END", Input.Keys.END, "End"),
            arrayOf("INSERT", Input.Keys.INSERT, "Ins"),
            arrayOf("PAGE_UP", Input.Keys.PAGE_UP, "P${0x18.toChar()}"),
            arrayOf("PAGE_DOWN", Input.Keys.PAGE_DOWN, "P${0x19.toChar()}"),
            arrayOf("PICTSYMBOLS", Input.Keys.PICTSYMBOLS, "_"),
            arrayOf("SWITCH_CHARSET", Input.Keys.SWITCH_CHARSET, "Lang"),
            arrayOf("BUTTON_CIRCLE", Input.Keys.BUTTON_CIRCLE, "_"),
            arrayOf("BUTTON_A", Input.Keys.BUTTON_A, "_"),
            arrayOf("BUTTON_B", Input.Keys.BUTTON_B, "_"),
            arrayOf("BUTTON_C", Input.Keys.BUTTON_C, "_"),
            arrayOf("BUTTON_X", Input.Keys.BUTTON_X, "_"),
            arrayOf("BUTTON_Y", Input.Keys.BUTTON_Y, "_"),
            arrayOf("BUTTON_Z", Input.Keys.BUTTON_Z, "_"),
            arrayOf("BUTTON_L1", Input.Keys.BUTTON_L1, "_"),
            arrayOf("BUTTON_R1", Input.Keys.BUTTON_R1, "_"),
            arrayOf("BUTTON_L2", Input.Keys.BUTTON_L2, "_"),
            arrayOf("BUTTON_R2", Input.Keys.BUTTON_R2, "_"),
            arrayOf("BUTTON_THUMBL", Input.Keys.BUTTON_THUMBL, "_"),
            arrayOf("BUTTON_THUMBR", Input.Keys.BUTTON_THUMBR, "_"),
            arrayOf("BUTTON_START", Input.Keys.BUTTON_START, "_"),
            arrayOf("BUTTON_SELECT", Input.Keys.BUTTON_SELECT, "_"),
            arrayOf("BUTTON_MODE", Input.Keys.BUTTON_MODE, "_")
    )

    val keycodeToLabel = HashMap<Int, String>(keyDict.size)

    init {
        keyDict.forEach {
            keycodeToLabel[it[1] as Int] = it[2] as String
        }
    }

}