package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.DefaultConfig
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2021-09-15.
 */
class UIKeyboardControlPanel : UICanvas() {

    init {
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override var width = 600
    override var height = 600
    override var openCloseTime = 0f

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    internal val kbx = drawX + 61
    internal val kby = drawY + 95

    private val oneu = 28
    private val onehalfu = 44
    private val twou = 52
    private val twohalfu = 68
    private val threeu = 84
    private val spaceu = 188

    private val borderNormal = Color(0xFFFFFF80.toInt())
    private val fillCol = Color(0x80)

    private val keycaps = hashMapOf(
            Input.Keys.GRAVE to UIItemKeycap(this, 1, 1, null, oneu, ""),
            Input.Keys.NUM_1 to UIItemKeycap(this, 33,1,  Input.Keys.NUM_1, oneu, "1,3"),
            Input.Keys.NUM_2 to UIItemKeycap(this, 65,1,  Input.Keys.NUM_2, oneu, "2,3"),
            Input.Keys.NUM_3 to UIItemKeycap(this, 97,1,  Input.Keys.NUM_3, oneu, "3,3"),
            Input.Keys.NUM_4 to UIItemKeycap(this, 129,1, Input.Keys.NUM_4, oneu, "4,3"),
            Input.Keys.NUM_5 to UIItemKeycap(this, 161,1, Input.Keys.NUM_5, oneu, "5,3"),
            Input.Keys.NUM_6 to UIItemKeycap(this, 193,1, Input.Keys.NUM_6, oneu, "6,3"),
            Input.Keys.NUM_7 to UIItemKeycap(this, 225,1, Input.Keys.NUM_7, oneu, "7,3"),
            Input.Keys.NUM_8 to UIItemKeycap(this, 257,1, Input.Keys.NUM_8, oneu, "8,3"),
            Input.Keys.NUM_9 to UIItemKeycap(this, 289,1, Input.Keys.NUM_9, oneu, "9,3"),
            Input.Keys.NUM_0 to UIItemKeycap(this, 321,1, Input.Keys.NUM_0, oneu, "0,3"),
            Input.Keys.MINUS to UIItemKeycap(this, 353,1, Input.Keys.MINUS, oneu, "10,3"),
            Input.Keys.EQUALS to UIItemKeycap(this, 385,1, Input.Keys.EQUALS, oneu, "11,3"),
            Input.Keys.BACKSPACE to UIItemKeycap(this, 417,1, Input.Keys.BACKSPACE, 60, "24,5"),

            Input.Keys.TAB to UIItemKeycap(this, 1,33,  Input.Keys.TAB, onehalfu, "23,5"),
            Input.Keys.Q to UIItemKeycap(this, 49,33,  Input.Keys.Q, oneu, "16,4"),
            Input.Keys.W to UIItemKeycap(this, 81,33,  Input.Keys.W, oneu, "22,4"),
            Input.Keys.E to UIItemKeycap(this, 113,33,  Input.Keys.E, oneu, "4,4"),
            Input.Keys.R to UIItemKeycap(this, 145,33,  Input.Keys.R, oneu, "17,4"),
            Input.Keys.T to UIItemKeycap(this, 177,33,  Input.Keys.T, oneu, "19,4"),
            Input.Keys.Y to UIItemKeycap(this, 209,33,  Input.Keys.Y, oneu, "24,4"),
            Input.Keys.U to UIItemKeycap(this, 241,33,  Input.Keys.U, oneu, "20,4"),
            Input.Keys.I to UIItemKeycap(this, 273,33,  Input.Keys.I, oneu, "8,4"),
            Input.Keys.O to UIItemKeycap(this, 305,33,  Input.Keys.O, oneu, "14,4"),
            Input.Keys.P to UIItemKeycap(this, 337,33,  Input.Keys.P, oneu, "15,4"),
            Input.Keys.LEFT_BRACKET to UIItemKeycap(this, 369,33,  Input.Keys.LEFT_BRACKET, oneu, "12,3"),
            Input.Keys.RIGHT_BRACKET to UIItemKeycap(this, 401,33,  Input.Keys.RIGHT_BRACKET, oneu, "13,3"),
            Input.Keys.BACKSLASH to UIItemKeycap(this, 433,33,  Input.Keys.BACKSLASH, onehalfu, "20,3"),

            Input.Keys.CAPS_LOCK to UIItemKeycap(this, 1,65,  Input.Keys.CAPS_LOCK, twou, "24,3"),
            Input.Keys.A to UIItemKeycap(this, 57,65,  Input.Keys.A, oneu, "0,4"),
            Input.Keys.S to UIItemKeycap(this, 89,65,  Input.Keys.S, oneu, "18,4"),
            Input.Keys.D to UIItemKeycap(this, 121,65,  Input.Keys.D, oneu, "3,4"),
            Input.Keys.F to UIItemKeycap(this, 153,65,  Input.Keys.F, oneu, "5,4", true),
            Input.Keys.G to UIItemKeycap(this, 185,65,  Input.Keys.G, oneu, "6,4"),
            Input.Keys.H to UIItemKeycap(this, 217,65,  Input.Keys.H, oneu, "7,4"),
            Input.Keys.J to UIItemKeycap(this, 249,65,  Input.Keys.J, oneu, "9,4", true),
            Input.Keys.K to UIItemKeycap(this, 281,65,  Input.Keys.K, oneu, "10,4"),
            Input.Keys.L to UIItemKeycap(this, 313,65,  Input.Keys.L, oneu, "11,4"),
            Input.Keys.SEMICOLON to UIItemKeycap(this, 345,65,  Input.Keys.SEMICOLON, oneu, "15,3"),
            Input.Keys.APOSTROPHE to UIItemKeycap(this, 377,65,  Input.Keys.APOSTROPHE, oneu, "16,3"),
            Input.Keys.ENTER to UIItemKeycap(this, 409,65,  Input.Keys.ENTER, twohalfu, "17,3"),

            Input.Keys.SHIFT_LEFT to UIItemKeycap(this, 1,97,  Input.Keys.SHIFT_LEFT, twohalfu, "23,3"),
            Input.Keys.Z to UIItemKeycap(this, 73,97,  Input.Keys.Z, oneu, "0,5"),
            Input.Keys.X to UIItemKeycap(this, 105,97,  Input.Keys.X, oneu, "23,4"),
            Input.Keys.C to UIItemKeycap(this, 137,97,  Input.Keys.C, oneu, "2,4"),
            Input.Keys.V to UIItemKeycap(this, 169,97,  Input.Keys.V, oneu, "21,4"),
            Input.Keys.B to UIItemKeycap(this, 201,97,  Input.Keys.B, oneu, "1,4"),
            Input.Keys.N to UIItemKeycap(this, 233,97,  Input.Keys.N, oneu, "13,4"),
            Input.Keys.M to UIItemKeycap(this, 265,97,  Input.Keys.M, oneu, "12,4"),
            Input.Keys.COMMA to UIItemKeycap(this, 297,97,  Input.Keys.COMMA, oneu, "18,3"),
            Input.Keys.PERIOD to UIItemKeycap(this, 329,97,  Input.Keys.PERIOD, oneu, "19,3"),
            Input.Keys.SLASH to UIItemKeycap(this, 361,97,  Input.Keys.SLASH, oneu, "20,3"),
            Input.Keys.SHIFT_RIGHT to UIItemKeycap(this, 393,97,  Input.Keys.SHIFT_RIGHT, threeu, "23,3"),

            Input.Keys.CONTROL_LEFT to UIItemKeycap(this, 1,129,  Input.Keys.CONTROL_LEFT, onehalfu, "21,3"),
            -2 to UIItemKeycap(this, 49,129,  null, oneu, ""),
            Input.Keys.ALT_LEFT to UIItemKeycap(this, 81,129,  Input.Keys.ALT_LEFT, onehalfu, "22,3"),
            Input.Keys.SPACE to UIItemKeycap(this, 129,129,  Input.Keys.SPACE, spaceu, ""),
            Input.Keys.ALT_RIGHT to UIItemKeycap(this, 321,129,  Input.Keys.ALT_RIGHT, onehalfu, "22,3"),
            -3 to UIItemKeycap(this, 369,129,  null, oneu, ""),
            -4 to UIItemKeycap(this, 401,129,  null, oneu, ""),
            Input.Keys.CONTROL_RIGHT to UIItemKeycap(this, 433,129,  Input.Keys.CONTROL_RIGHT, onehalfu, "21,3"),

    ) // end of keycaps

    private val buttonReset = UIItemTextButton(this, "MENU_LABEL_RESET", kbx + 1, kby + 165, 180, true, alignment = UIItemTextButton.Companion.Alignment.LEFT)

    private val symbolLeft = labels.get(0,2)
    private val symbolUp = labels.get(1,2)
    private val symbolRight = labels.get(2,2)
    private val symbolDown = labels.get(3,2)
    private val symbolJump = labels.get(4,2)
    private val symbolZoom = labels.get(5,2)
    private val symbolInventory = labels.get(9,0)
    private val symbolGrapplingHook = labels.get(5,1)
    private val symbolGamemenu = labels.get(6,2)

    private val controlPalette = UIItemControlPaletteBaloon(this, (Toolkit.drawWidth - 480) / 2, kby + 219)

    init {
        keycaps.values.forEach { addUIitem(it) }
        updateKeycaps()

        buttonReset.clickOnceListener = { x, y, button ->
            println("reset keys!")
            resetKeyConfig()
            updateKeycaps()
        }
    }

    private fun resetKeyConfig() {
        listOf("control_key_up",
                "control_key_left",
                "control_key_down",
                "control_key_right",
                "control_key_jump",
                "control_key_zoom",
                "control_key_inventory",
                "control_key_movementaux",
                "control_key_gamemenu").forEach {

                    App.setConfig(it, DefaultConfig.hashMap[it]!! as Int)
        }
    }

    private fun updateKeycaps() {
        keycaps.values.forEach { it.symbolControl = null }
        // read config and put icons
        keycaps[App.getConfigInt("control_key_up")]?.symbolControl = symbolUp
        keycaps[App.getConfigInt("control_key_left")]?.symbolControl = symbolLeft
        keycaps[App.getConfigInt("control_key_down")]?.symbolControl = symbolDown
        keycaps[App.getConfigInt("control_key_right")]?.symbolControl = symbolRight

        keycaps[App.getConfigInt("control_key_jump")]?.symbolControl = symbolJump
        keycaps[App.getConfigInt("control_key_zoom")]?.symbolControl = symbolZoom
        keycaps[App.getConfigInt("control_key_inventory")]?.symbolControl = symbolInventory
//        keycaps[App.getConfigInt("control_key_movementaux")]?.symbolControl = symbolGrapplingHook

        keycaps[App.getConfigInt("control_key_gamemenu")]?.symbolControl = symbolGamemenu
    }

    internal var keycapClicked = -13372
        set(value) {
            if (field != value) keycaps[field]?.selected = false
            field = value
        }
    internal var controlSelected = -1

    override fun updateUI(delta: Float) {
        uiItems.forEach {
            it.update(delta)
            if (it is UIItemKeycap && it.mousePushed) {
                it.selected = true
//                println("key ${it.key}; selected = ${it.selected}")
                keycapClicked = it.key ?: -13372
            }
        }

        buttonReset.update(delta)

        if (keycapClicked >= 0 && controlSelected < 0) {
            controlPalette.update(delta)
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
//        batch.color = borderNormal
//        Toolkit.drawBoxBorder(batch, drawX, drawY, width, height)
//        batch.color = fillCol
//        Toolkit.fillArea(batch, drawX, drawY, width, height)
        uiItems.forEach { it.render(batch, camera) }
        buttonReset.render(batch, camera)

        batch.color = Color.WHITE

        if (keycapClicked >= 0 && controlSelected < 0) {
            controlPalette.render(batch, camera)
        }


        val title = Lang["MENU_CONTROLS_KEYBOARD"]
        App.fontGame.draw(batch, title, drawX.toFloat() + (width - App.fontGame.getWidth(title)) / 2, drawY.toFloat())

    }

    fun setControlOf(key: Int, control: Int) {
        if (control >= 0) {
            App.setConfig(UIItemControlPaletteBaloon.indexToConfigKey[control]!!, key)
        }
        updateKeycaps()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        buttonReset.touchDown(screenX, screenY, pointer, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        buttonReset.touchUp(screenX, screenY, pointer, button)
        return true
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }


}



/**
 * @param key LibGDX keycode. Set it to `null` to "disable" the key. Also see [com.badlogic.gdx.Input.Keys]
 */
class UIItemKeycap(
        parent: UIKeyboardControlPanel,
        initialX: Int,
        initialY: Int,
        val key: Int?,
        override val width: Int,
        symbolDefault: String,
        val homerow: Boolean = false
) : UIItem(parent, initialX, initialY) {

    init {
        this.posX = initialX + parent.kbx
        this.posY = initialY + parent.kby
    }

    override val height = 28

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    private val s1 = if (symbolDefault.isNotBlank()) symbolDefault.split(',').map { it.toInt() } else null

    var symbolControl: TextureRegion? = null
    var selected = false

    private val borderKeyForbidden = Color(0x000000C0)
    private val borderKeyNormal = Color(0xFFFFFFAA.toInt())
    private val borderMouseUp = UIItemTextButton.defaultActiveCol
    private val borderKeyPressed = UIItemTextButton.defaultHighlightCol
    private val borderKeyPressedAndSelected = Color(0x33FF33FF.toInt())

    private val keycapFill = Color(0x404040_C0)

    private val keylabelCol = Color(0xFFFFFF40.toInt())
    private val configuredKeyCol = Color.WHITE

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        batch.color = if (key == null)
            borderKeyForbidden
        else if (Gdx.input.isKeyPressed(key) && selected)
            borderKeyPressedAndSelected
        else if (Gdx.input.isKeyPressed(key) || selected)
            borderKeyPressed
        else if (mouseUp)
            borderMouseUp
        else
            borderKeyNormal

        // key border
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)

        if (homerow) {
            Toolkit.drawBoxBorder(batch, posX + 9, posY + 26, 10, 1)
        }

        // key background
        batch.color = keycapFill
        Toolkit.fillArea(batch, posX, posY, width, height)

        // default keycap
        if (symbolControl != null) {
            batch.color = configuredKeyCol
            batch.draw(symbolControl, (posX + (width - 20) / 2).toFloat(), posY + 4f)
        }
        else if (s1 != null) {
            batch.color = keylabelCol
            batch.draw(labels.get(s1[0], s1[1]), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        }
    }

    override fun dispose() {
    }
}

class UIItemControlPaletteBaloon(val parent: UIKeyboardControlPanel, initialX: Int, initialY: Int) : UIItem(parent, initialX, initialY) {
    override val width = 480
    override val height = 230
    override fun dispose() {}

    private val icons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    private val buttonBackground = Color(0x2C2C2CFF)

    private val iconButtons = arrayOf(
            // left up right down
            UIItemImageButton(parent, icons.get(0,2), initialX = initialX + 154, initialY = initialY + 40, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            UIItemImageButton(parent, icons.get(1,2), initialX = initialX + 188, initialY = initialY + 23, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            UIItemImageButton(parent, icons.get(2,2), initialX = initialX + 222, initialY = initialY + 40, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            UIItemImageButton(parent, icons.get(3,2), initialX = initialX + 188, initialY = initialY + 57, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            // jump
            UIItemImageButton(parent, icons.get(4,2), initialX = initialX + 50, initialY = initialY + 100, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            // inventory
            UIItemImageButton(parent, icons.get(9,0), initialX = initialX + 50, initialY = initialY + 140, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            // zoom
            UIItemImageButton(parent, icons.get(5,2), initialX = initialX + (width / 2) + 20, initialY = initialY + 100, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
            // system menu
            UIItemImageButton(parent, icons.get(6,2), initialX = initialX + (width / 2) + 20, initialY = initialY + 140, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0)),
    )

    // close button is just for the cosmetics; the uiitem closes when you click anywhere on the UI
    private val closeButton =
            UIItemImageButton(parent, icons.get(22,0), initialX = initialX + width - 20, initialY = initialY, highlightable = false, backgroundCol = Color(0), activeBackCol = Color(0), highlightBackCol = Color(0))

    companion object {
        val indexToConfigKey = hashMapOf(
                0 to "control_key_left",
                1 to "control_key_up",
                2 to "control_key_right",
                3 to "control_key_down",
                4 to "control_key_jump",
                5 to "control_key_inventory",
                6 to "control_key_zoom",
                7 to "control_key_gamemenu"
        )
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        Toolkit.drawBaloon(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

        iconButtons.forEach {
            batch.color = buttonBackground
            Toolkit.fillArea(batch, it.posX-4, it.posY-4, 28, 28)
            it.render(batch, camera)
            batch.color = Color(batch.color.r, batch.color.g, batch.color.b, batch.color.a * (if (it.mouseUp) 0.9f else 0.6f))
            Toolkit.drawBoxBorder(batch, it.posX-4, it.posY-4, 28, 28)
        }

        closeButton.render(batch, camera)

        // texts
        batch.color = Color.WHITE
        App.fontGame.draw(batch, Lang["GAME_ACTION_MOVE_VERB"], posX + 262f, posY + 40f)

        App.fontGame.draw(batch, Lang["GAME_ACTION_JUMP"], posX + 90f, posY + 100f)
        App.fontGame.draw(batch, Lang["GAME_INVENTORY"], posX + 90f, posY + 140f)

        App.fontGame.draw(batch, Lang["GAME_ACTION_ZOOM"], posX + (width / 2) + 60f, posY + 100f)
        App.fontGame.draw(batch, Lang["MENU_LABEL_MENU"], posX + (width / 2) + 60f, posY + 140f)
    }


    override fun update(delta: Float) {
        super.update(delta) // unlatches mouse
        var selected = -1

        iconButtons.forEachIndexed { index, it ->
            it.update(delta)
            if (it.mousePushed) {
                selected  = index
            }
        }

        closeButton.update(delta)

        // close
        if (!mouseLatched && mousePushed) {
            mouseLatched = true
            parent.setControlOf(parent.keycapClicked, selected)
            parent.keycapClicked = -13372
        }
    }
}