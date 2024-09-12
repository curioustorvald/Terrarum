package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.unicode.EMDASH
import net.torvald.terrarum.gamecontroller.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*

/**
 * Created by minjaesong on 2021-11-10.
 */
class UIIMEConfig(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 480
    override var height = 600


    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    internal val kbx = drawX + 1
    internal val kby = drawY + 95


    companion object {
        private val oneu = 28
        private val onequartu = 36
        private val onehalfu = 44
        private val twou = 52
        private val twoquartu = 60
        private val twohalfu = 68
        private val threeu = 84
        private val spaceu = 188

        private fun getKeycapsANSI(parent: UIIMEConfig) = hashMapOf(
            Input.Keys.GRAVE to UIItemInputKeycap(parent, 1, 1, Input.Keys.GRAVE, oneu),
            Input.Keys.NUM_1 to UIItemInputKeycap(parent, 33,1,  Input.Keys.NUM_1, oneu),
            Input.Keys.NUM_2 to UIItemInputKeycap(parent, 65,1,  Input.Keys.NUM_2, oneu),
            Input.Keys.NUM_3 to UIItemInputKeycap(parent, 97,1,  Input.Keys.NUM_3, oneu),
            Input.Keys.NUM_4 to UIItemInputKeycap(parent, 129,1, Input.Keys.NUM_4, oneu),
            Input.Keys.NUM_5 to UIItemInputKeycap(parent, 161,1, Input.Keys.NUM_5, oneu),
            Input.Keys.NUM_6 to UIItemInputKeycap(parent, 193,1, Input.Keys.NUM_6, oneu),
            Input.Keys.NUM_7 to UIItemInputKeycap(parent, 225,1, Input.Keys.NUM_7, oneu),
            Input.Keys.NUM_8 to UIItemInputKeycap(parent, 257,1, Input.Keys.NUM_8, oneu),
            Input.Keys.NUM_9 to UIItemInputKeycap(parent, 289,1, Input.Keys.NUM_9, oneu),
            Input.Keys.NUM_0 to UIItemInputKeycap(parent, 321,1, Input.Keys.NUM_0, oneu),
            Input.Keys.MINUS to UIItemInputKeycap(parent, 353,1, Input.Keys.MINUS, oneu),
            Input.Keys.EQUALS to UIItemInputKeycap(parent, 385,1, Input.Keys.EQUALS, oneu),
            Input.Keys.BACKSPACE to UIItemInputKeycap(parent, 417,1, Input.Keys.BACKSPACE, twoquartu),

            Input.Keys.TAB to UIItemInputKeycap(parent, 1,33,  Input.Keys.TAB, onehalfu),
            Input.Keys.Q to UIItemInputKeycap(parent, 49,33,  Input.Keys.Q, oneu),
            Input.Keys.W to UIItemInputKeycap(parent, 81,33,  Input.Keys.W, oneu),
            Input.Keys.E to UIItemInputKeycap(parent, 113,33,  Input.Keys.E, oneu),
            Input.Keys.R to UIItemInputKeycap(parent, 145,33,  Input.Keys.R, oneu),
            Input.Keys.T to UIItemInputKeycap(parent, 177,33,  Input.Keys.T, oneu),
            Input.Keys.Y to UIItemInputKeycap(parent, 209,33,  Input.Keys.Y, oneu),
            Input.Keys.U to UIItemInputKeycap(parent, 241,33,  Input.Keys.U, oneu),
            Input.Keys.I to UIItemInputKeycap(parent, 273,33,  Input.Keys.I, oneu),
            Input.Keys.O to UIItemInputKeycap(parent, 305,33,  Input.Keys.O, oneu),
            Input.Keys.P to UIItemInputKeycap(parent, 337,33,  Input.Keys.P, oneu),
            Input.Keys.LEFT_BRACKET to UIItemInputKeycap(parent, 369,33,  Input.Keys.LEFT_BRACKET, oneu),
            Input.Keys.RIGHT_BRACKET to UIItemInputKeycap(parent, 401,33,  Input.Keys.RIGHT_BRACKET, oneu),
            Input.Keys.BACKSLASH to UIItemInputKeycap(parent, 433,33,  Input.Keys.BACKSLASH, onehalfu),

            Input.Keys.CAPS_LOCK to UIItemInputKeycap(parent, 1,65,  Input.Keys.CAPS_LOCK, twou),
            Input.Keys.A to UIItemInputKeycap(parent, 57,65,  Input.Keys.A, oneu),
            Input.Keys.S to UIItemInputKeycap(parent, 89,65,  Input.Keys.S, oneu),
            Input.Keys.D to UIItemInputKeycap(parent, 121,65,  Input.Keys.D, oneu),
            Input.Keys.F to UIItemInputKeycap(parent, 153,65,  Input.Keys.F, oneu, true),
            Input.Keys.G to UIItemInputKeycap(parent, 185,65,  Input.Keys.G, oneu),
            Input.Keys.H to UIItemInputKeycap(parent, 217,65,  Input.Keys.H, oneu),
            Input.Keys.J to UIItemInputKeycap(parent, 249,65,  Input.Keys.J, oneu, true),
            Input.Keys.K to UIItemInputKeycap(parent, 281,65,  Input.Keys.K, oneu),
            Input.Keys.L to UIItemInputKeycap(parent, 313,65,  Input.Keys.L, oneu),
            Input.Keys.SEMICOLON to UIItemInputKeycap(parent, 345,65,  Input.Keys.SEMICOLON, oneu),
            Input.Keys.APOSTROPHE to UIItemInputKeycap(parent, 377,65,  Input.Keys.APOSTROPHE, oneu),
            Input.Keys.ENTER to UIItemInputKeycap(parent, 409,65,  Input.Keys.ENTER, twohalfu),

            Input.Keys.SHIFT_LEFT to UIItemInputKeycap(parent, 1,97,  Input.Keys.SHIFT_LEFT, twohalfu),
            Input.Keys.Z to UIItemInputKeycap(parent, 73,97,  Input.Keys.Z, oneu),
            Input.Keys.X to UIItemInputKeycap(parent, 105,97,  Input.Keys.X, oneu),
            Input.Keys.C to UIItemInputKeycap(parent, 137,97,  Input.Keys.C, oneu),
            Input.Keys.V to UIItemInputKeycap(parent, 169,97,  Input.Keys.V, oneu),
            Input.Keys.B to UIItemInputKeycap(parent, 201,97,  Input.Keys.B, oneu),
            Input.Keys.N to UIItemInputKeycap(parent, 233,97,  Input.Keys.N, oneu),
            Input.Keys.M to UIItemInputKeycap(parent, 265,97,  Input.Keys.M, oneu),
            Input.Keys.COMMA to UIItemInputKeycap(parent, 297,97,  Input.Keys.COMMA, oneu),
            Input.Keys.PERIOD to UIItemInputKeycap(parent, 329,97,  Input.Keys.PERIOD, oneu),
            Input.Keys.SLASH to UIItemInputKeycap(parent, 361,97,  Input.Keys.SLASH, oneu),
            Input.Keys.SHIFT_RIGHT to UIItemInputKeycap(parent, 393,97,  Input.Keys.SHIFT_RIGHT, threeu),

            Input.Keys.CONTROL_LEFT to UIItemInputKeycap(parent, 1,129,  Input.Keys.CONTROL_LEFT, onehalfu),
            -2 to UIItemInputKeycap(parent, 49,129,  null, oneu),
            Input.Keys.ALT_LEFT to UIItemInputKeycap(parent, 81,129,  Input.Keys.ALT_LEFT, onehalfu),
            Input.Keys.SPACE to UIItemInputKeycap(parent, 129,129,  Input.Keys.SPACE, spaceu),
            Input.Keys.ALT_RIGHT to UIItemInputKeycap(parent, 321,129,  Input.Keys.ALT_RIGHT, onehalfu),
            -3 to UIItemInputKeycap(parent, 369,129,  null, oneu),
            -4 to UIItemInputKeycap(parent, 401,129,  null, oneu),
            Input.Keys.CONTROL_RIGHT to UIItemInputKeycap(parent, 433,129,  Input.Keys.CONTROL_RIGHT, onehalfu),

        ) // end of keycaps


        private fun getKeycapsISO(parent: UIIMEConfig) = hashMapOf(
            Input.Keys.GRAVE to UIItemInputKeycap(parent, 1, 1, Input.Keys.GRAVE, oneu),
            Input.Keys.NUM_1 to UIItemInputKeycap(parent, 33,1,  Input.Keys.NUM_1, oneu),
            Input.Keys.NUM_2 to UIItemInputKeycap(parent, 65,1,  Input.Keys.NUM_2, oneu),
            Input.Keys.NUM_3 to UIItemInputKeycap(parent, 97,1,  Input.Keys.NUM_3, oneu),
            Input.Keys.NUM_4 to UIItemInputKeycap(parent, 129,1, Input.Keys.NUM_4, oneu),
            Input.Keys.NUM_5 to UIItemInputKeycap(parent, 161,1, Input.Keys.NUM_5, oneu),
            Input.Keys.NUM_6 to UIItemInputKeycap(parent, 193,1, Input.Keys.NUM_6, oneu),
            Input.Keys.NUM_7 to UIItemInputKeycap(parent, 225,1, Input.Keys.NUM_7, oneu),
            Input.Keys.NUM_8 to UIItemInputKeycap(parent, 257,1, Input.Keys.NUM_8, oneu),
            Input.Keys.NUM_9 to UIItemInputKeycap(parent, 289,1, Input.Keys.NUM_9, oneu),
            Input.Keys.NUM_0 to UIItemInputKeycap(parent, 321,1, Input.Keys.NUM_0, oneu),
            Input.Keys.MINUS to UIItemInputKeycap(parent, 353,1, Input.Keys.MINUS, oneu),
            Input.Keys.EQUALS to UIItemInputKeycap(parent, 385,1, Input.Keys.EQUALS, oneu),
            Input.Keys.BACKSPACE to UIItemInputKeycap(parent, 417,1, Input.Keys.BACKSPACE, twoquartu),

            Input.Keys.TAB to UIItemInputKeycap(parent, 1,33,  Input.Keys.TAB, onehalfu),
            Input.Keys.Q to UIItemInputKeycap(parent, 49,33,  Input.Keys.Q, oneu),
            Input.Keys.W to UIItemInputKeycap(parent, 81,33,  Input.Keys.W, oneu),
            Input.Keys.E to UIItemInputKeycap(parent, 113,33,  Input.Keys.E, oneu),
            Input.Keys.R to UIItemInputKeycap(parent, 145,33,  Input.Keys.R, oneu),
            Input.Keys.T to UIItemInputKeycap(parent, 177,33,  Input.Keys.T, oneu),
            Input.Keys.Y to UIItemInputKeycap(parent, 209,33,  Input.Keys.Y, oneu),
            Input.Keys.U to UIItemInputKeycap(parent, 241,33,  Input.Keys.U, oneu),
            Input.Keys.I to UIItemInputKeycap(parent, 273,33,  Input.Keys.I, oneu),
            Input.Keys.O to UIItemInputKeycap(parent, 305,33,  Input.Keys.O, oneu),
            Input.Keys.P to UIItemInputKeycap(parent, 337,33,  Input.Keys.P, oneu),
            Input.Keys.LEFT_BRACKET to UIItemInputKeycap(parent, 369,33,  Input.Keys.LEFT_BRACKET, oneu),
            Input.Keys.RIGHT_BRACKET to UIItemInputKeycap(parent, 401,33,  Input.Keys.RIGHT_BRACKET, oneu),
            Input.Keys.ENTER to UIItemInputKeycapTwoRowRight(parent, 433,33,  Input.Keys.ENTER, onehalfu, onequartu),

            Input.Keys.CAPS_LOCK to UIItemInputKeycap(parent, 1,65,  Input.Keys.CAPS_LOCK, twou),
            Input.Keys.A to UIItemInputKeycap(parent, 57,65,  Input.Keys.A, oneu),
            Input.Keys.S to UIItemInputKeycap(parent, 89,65,  Input.Keys.S, oneu),
            Input.Keys.D to UIItemInputKeycap(parent, 121,65,  Input.Keys.D, oneu),
            Input.Keys.F to UIItemInputKeycap(parent, 153,65,  Input.Keys.F, oneu, true),
            Input.Keys.G to UIItemInputKeycap(parent, 185,65,  Input.Keys.G, oneu),
            Input.Keys.H to UIItemInputKeycap(parent, 217,65,  Input.Keys.H, oneu),
            Input.Keys.J to UIItemInputKeycap(parent, 249,65,  Input.Keys.J, oneu, true),
            Input.Keys.K to UIItemInputKeycap(parent, 281,65,  Input.Keys.K, oneu),
            Input.Keys.L to UIItemInputKeycap(parent, 313,65,  Input.Keys.L, oneu),
            Input.Keys.SEMICOLON to UIItemInputKeycap(parent, 345,65,  Input.Keys.SEMICOLON, oneu),
            Input.Keys.APOSTROPHE to UIItemInputKeycap(parent, 377,65,  Input.Keys.APOSTROPHE, oneu),
            Input.Keys.BACKSLASH to UIItemInputKeycap(parent, 409,65,  Input.Keys.BACKSLASH, oneu),

            Input.Keys.SHIFT_LEFT to UIItemInputKeycap(parent, 1,97,  Input.Keys.SHIFT_LEFT, onequartu),
            Input.Keys.WORLD_1 to UIItemInputKeycap(parent, 41,97,  Input.Keys.WORLD_1, oneu),
            Input.Keys.Z to UIItemInputKeycap(parent, 73,97,  Input.Keys.Z, oneu),
            Input.Keys.X to UIItemInputKeycap(parent, 105,97,  Input.Keys.X, oneu),
            Input.Keys.C to UIItemInputKeycap(parent, 137,97,  Input.Keys.C, oneu),
            Input.Keys.V to UIItemInputKeycap(parent, 169,97,  Input.Keys.V, oneu),
            Input.Keys.B to UIItemInputKeycap(parent, 201,97,  Input.Keys.B, oneu),
            Input.Keys.N to UIItemInputKeycap(parent, 233,97,  Input.Keys.N, oneu),
            Input.Keys.M to UIItemInputKeycap(parent, 265,97,  Input.Keys.M, oneu),
            Input.Keys.COMMA to UIItemInputKeycap(parent, 297,97,  Input.Keys.COMMA, oneu),
            Input.Keys.PERIOD to UIItemInputKeycap(parent, 329,97,  Input.Keys.PERIOD, oneu),
            Input.Keys.SLASH to UIItemInputKeycap(parent, 361,97,  Input.Keys.SLASH, oneu),
            Input.Keys.SHIFT_RIGHT to UIItemInputKeycap(parent, 393,97,  Input.Keys.SHIFT_RIGHT, threeu),

            Input.Keys.CONTROL_LEFT to UIItemInputKeycap(parent, 1,129,  Input.Keys.CONTROL_LEFT, onehalfu),
            -2 to UIItemInputKeycap(parent, 49,129,  null, oneu),
            Input.Keys.ALT_LEFT to UIItemInputKeycap(parent, 81,129,  Input.Keys.ALT_LEFT, onehalfu),
            Input.Keys.SPACE to UIItemInputKeycap(parent, 129,129,  Input.Keys.SPACE, spaceu),
            Input.Keys.ALT_RIGHT to UIItemInputKeycap(parent, 321,129,  Input.Keys.ALT_RIGHT, onehalfu),
            -3 to UIItemInputKeycap(parent, 369,129,  null, oneu),
            -4 to UIItemInputKeycap(parent, 401,129,  null, oneu),
            Input.Keys.CONTROL_RIGHT to UIItemInputKeycap(parent, 433,129,  Input.Keys.CONTROL_RIGHT, onehalfu),

        ) // end of keycaps
    }

    private lateinit var keycaps: HashMap<Int, UIItemInputKeycap>

    init {
        handler.allowESCtoClose = false
    }

    private fun refreshKeycaps(layout: String) {
        if (::keycaps.isInitialized)
            keycaps.values.forEach { removeUIitem(it) }

        keycaps = when (layout) {
            "ansi" -> getKeycapsANSI(this)
            "iso" -> getKeycapsISO(this)
            else -> throw IllegalArgumentException("Unknown physical layout: $layout")
            // the JIS "Ro" key is not even recognised as a key by GLFW so JIS layout cannot be supported even if I wanted to
            // besides, nobody in Japan types in Kana layout any more
        }

        keycaps.values.forEach { addUIitemAtHead(it) }
    }

    private val textSelWidth = 266
    private val selectorWidth = 600
    private val halfselw = selectorWidth / 2
    private val selDrawX = (Toolkit.drawWidth - selectorWidth) / 2
    private val halfw = width / 2

//    private val y1 = 400
//    private val y2 = y1 + 40

    private val lowLayerCodes = IME.getAllLowLayers().sorted()
    private val lowLayerNames = lowLayerCodes.map { { IME.getLowLayerByName(it).name } }
    private val keyboardLayoutSelection = UIItemTextSelector(this,
            selDrawX + (halfselw - textSelWidth) / 2,
        kby + 260,
            lowLayerNames,
            lowLayerCodes.linearSearch { it == App.getConfigString("basekeyboardlayout") } ?: throw IME.LayoutNotFound(App.getConfigString("basekeyboardlayout")),
            textSelWidth
    )

    private val imeCodes0 = IME.getAllHighLayers().sorted()
    private val imeCodes = listOf("none") + imeCodes0
    private val imeNames = listOf({"$EMDASH"}) + imeCodes0.map { { IME.getHighLayerByName(it).name } }
    private val imeSelection = UIItemTextSelector(this,
        selDrawX + halfselw + (halfselw - textSelWidth) / 2,
        kby + 260,
            imeNames,
            imeCodes.linearSearch { it == App.getConfigString("inputmethod") } ?: throw IME.LayoutNotFound(App.getConfigString("inputmethod")),
            textSelWidth
    )



    private val keyboardTestPanel = UIItemTextLineInput(this,
            drawX + (width - 480) / 2 + 3,
            drawY + height - 120,
            474
    )


    private var oldPhysicalLayout = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout")).physicalLayout

    init {
        refreshKeycaps(oldPhysicalLayout)


        keyboardLayoutSelection.selectionChangeListener = {
            App.setConfig("basekeyboardlayout", lowLayerCodes[it])
        }
        imeSelection.selectionChangeListener = {
            App.setConfig("inputmethod", imeCodes[it])
        }

        addUIitem(keyboardTestPanel)
        addUIitem(keyboardLayoutSelection)
        addUIitem(imeSelection)
    }

    override fun updateImpl(delta: Float) {
        keyboardTestPanel.mouseoverUpdateLatch =
                (!keyboardLayoutSelection.paletteShowing &&
                 !imeSelection.paletteShowing)

        uiItems.forEach { it.update(delta) }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE

        val txt1 = Lang["MENU_LABEL_KEYBOARD_LAYOUT", true]; val tw1 = App.fontGame.getWidth(txt1)
        App.fontGame.draw(batch, txt1, selDrawX + (halfselw - tw1) / 2, keyboardLayoutSelection.posY - 40)

        val txt2 = Lang["MENU_LABEL_IME", true]; val tw2 = App.fontGame.getWidth(txt2)
        App.fontGame.draw(batch, txt2, selDrawX + halfselw + (halfselw - tw2) / 2, keyboardLayoutSelection.posY - 40)

        // title
        // todo show "Keyboard"/"Gamepad" accordingly
        val title = Lang["MENU_CONTROLS_KEYBOARD", true]
        batch.color = Color.WHITE
        App.fontUITitle.draw(batch, title, drawX.toFloat() + (width - App.fontUITitle.getWidth(title)) / 2, drawY.toFloat())


        // button help for string input UI
        val help1 = "￬ ${Lang["MENU_LABEL_IME_TOGGLE"]}"
        App.fontGame.draw(batch, help1, drawX + 10f, keyboardTestPanel.posY - 28f)
        val help2 = "${Lang["MENU_LABEL_PASTE_FROM_CLIPBOARD"]} ￪"
        App.fontGame.draw(batch, help2, drawX + keyboardTestPanel.width - 4f - App.fontGame.getWidth(help2), keyboardTestPanel.posY + 30f)



        batch.color = Color.WHITE
        uiItems.forEach { it.render(frameDelta, batch, camera) }

        shiftin = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
        altgrin = Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT) || (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
        lowlayer = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))
        highlayer = getIME()

        detectLowLayerChange()
    }

    private fun detectLowLayerChange() {
        val newPhysicalLayout = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout")).physicalLayout
        if (newPhysicalLayout != oldPhysicalLayout) {
            oldPhysicalLayout = newPhysicalLayout
            refreshKeycaps(newPhysicalLayout)
        }
    }

    internal var shiftin = false; private set
    internal var altgrin = false; private set
    internal var lowlayer = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout")); private set
    internal var highlayer: TerrarumIME? = null

    private fun getIME(): TerrarumIME? {
        val selectedIME = App.getConfigString("inputmethod")

        if (selectedIME == "none") return null
        try {
            return IME.getHighLayerByName(selectedIME)
        }
        catch (e: NullPointerException) {
            return null
        }
    }

    override fun dispose() {
    }
}



/**
 * @param key LibGDX keycode. Set it to `null` to "disable" the key. Also see [com.badlogic.gdx.Input.Keys]
 */
open private class UIItemInputKeycap(
        val parent: UIIMEConfig,
        initialX: Int,
        initialY: Int,
        val key: Int?,
        override val width: Int,
        val homerow: Boolean = false
) : UIItem(parent, initialX, initialY) {

    init {
        this.posX = initialX + parent.kbx
        this.posY = initialY + parent.kby
    }

    override val height = 28

    protected val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    var selected = false

    protected val borderKeyForbidden = Color(0x000000C0)
    protected val borderKeyNormal = Toolkit.Theme.COL_INACTIVE
    protected val borderMouseUp = Toolkit.Theme.COL_MOUSE_UP
    protected val borderKeyPressed = Toolkit.Theme.COL_SELECTED
    protected val borderKeyPressedAndSelected = Color(0x33FF33FF.toInt())

    protected val keycapFill = Toolkit.Theme.COL_CELL_FILL

    protected val keylabelCol = Color(0xddddddff.toInt())
    protected val configuredKeyCol = Color.WHITE

    override fun update(delta: Float) {
        super.update(delta)
    }

    private fun isDiacritic(c: Int) = c in 0x300..0x36F || c in 0x1AB0..0x1AFF ||
            c in 0x1DC0..0x1DFF || c in 0x20D0..0x20FF || c in 0xFE20..0xFE2F ||
            c == 0xE31 || c in 0xE33..0xE3A || c in 0xE47..0xE4E

    protected fun getBorderCol() = if (key == null)
        borderKeyForbidden
    else if (Gdx.input.isKeyPressed(key))
        borderKeyPressed
    else
        borderKeyNormal

    protected open fun drawKeycap(batch: SpriteBatch) {
        // key background
        batch.color = keycapFill
        Toolkit.fillArea(batch, posX, posY, width, height)

        // key border
        batch.color = getBorderCol()
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)
        if (homerow) Toolkit.drawBoxBorder(batch, posX + 9, posY + 26, 10, 1)
    }

    protected open fun getKeysymPos() = (posX + (width - 20) / 2).toFloat() to posY + 4f

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        super.render(frameDelta, batch, camera)

        drawKeycap(batch)
        val (symx, symy) = getKeysymPos()

        // keysym
        if (key == Input.Keys.CONTROL_LEFT || key == Input.Keys.CONTROL_RIGHT)
            batch.draw(labels.get(21,3), symx, symy)
        else if (key == Input.Keys.ALT_LEFT)
            batch.draw(labels.get(22,3), symx, symy)
        else if (key == Input.Keys.ALT_RIGHT)
            batch.draw(labels.get(23,2), symx, symy)
        else if (key == Input.Keys.SHIFT_LEFT || key == Input.Keys.SHIFT_RIGHT)
            batch.draw(labels.get(23,3), symx, symy)
        else if (key == Input.Keys.TAB)
            batch.draw(labels.get(23,5), symx, symy)
        else if (key == Input.Keys.BACKSPACE)
            batch.draw(labels.get(24,5), symx, symy)
        else if (key == Input.Keys.CAPS_LOCK) {
            if (parent.lowlayer.capsMode == TerrarumKeyCapsMode.CAPS)
                batch.draw(labels.get(24,3), symx, symy)
            else if (parent.lowlayer.capsMode == TerrarumKeyCapsMode.SHIFT)
                batch.draw(labels.get(24,2), symx, symy)
            else if (parent.lowlayer.capsMode == TerrarumKeyCapsMode.BACK)
                batch.draw(labels.get(24,5), symx, symy)
        }
        else if (key == Input.Keys.ENTER)
            batch.draw(labels.get(17,3), symx, symy)
        else if (key != null) {
            val keysymsLow = parent.lowlayer.symbols[key]
            val keysymLow =
                (if (parent.shiftin && parent.altgrin && keysymsLow[3]?.isNotEmpty() == true) keysymsLow[3]
                else if (parent.altgrin && keysymsLow[2]?.isNotEmpty() == true) keysymsLow[2]
                else if (parent.shiftin && keysymsLow[1]?.isNotEmpty() == true) keysymsLow[1]
                else keysymsLow[0]) ?: ""

            val keysym0: Array<String?> = if (KeyToggler.isOn(ControlPresets.getKey("control_key_toggleime"))) {
                if (parent.highlayer == null) arrayOf(keysymLow,keysymLow,keysymLow,keysymLow)
                else {
                    val keysyms = parent.highlayer!!.config.symbols
                    val keysymfun = parent.highlayer!!.config.symbolsfun

                    if (keysymfun != null) {
                        val ksym = keysymfun[keysymLow]
                        arrayOf(ksym,ksym,ksym,ksym)
                    }
                    else {
                        keysyms!!.get(key)
                    }
                }
            }
            else
                parent.lowlayer.symbols[key]

            var keysym =
                (if (parent.shiftin && parent.altgrin && keysym0[3]?.isNotEmpty() == true) keysym0[3]
                else if (parent.altgrin && keysym0[2]?.isNotEmpty() == true) keysym0[2]
                else if (parent.shiftin && keysym0[1]?.isNotEmpty() == true) keysym0[1]
                else keysym0[0]) ?: keysymLow

            if (keysym.isEmpty()) {
                return
            }

            if (isDiacritic(keysym[0].code))
                keysym = "\uDBBF\uDE01$keysym"

            if (keysym[0].code == 0xA0)
                batch.draw(labels.get(22, 2), symx, symy)
            else if (keysym[0].code == 0x20)
                batch.draw(labels.get(21,2), symx, symy)
            else {
                val keysymw = App.fontGame.getWidth(keysym)
                App.fontGame.draw(batch, keysym, posX + (width - keysymw) / 2, posY + 2)
            }
        }

    }

    override fun dispose() {
    }
}

private class UIItemInputKeycapTwoRowRight(
    parent: UIIMEConfig,
    initialX: Int,
    initialY: Int,
    key: Int?,
    val widthUp: Int,
    val widthDown: Int
) : UIItemInputKeycap(parent, initialX, initialY, key, maxOf(widthUp, widthDown), false) {
    init {
        this.posX = initialX + parent.kbx
        this.posY = initialY + parent.kby
    }

    private val rowheight = 28
    override val height = 60

    private val heightUp = if (widthUp > widthDown) 28 else 32
    private val heightDown = if (widthUp > widthDown) 32 else 28

    private val posXgap = (widthUp - widthDown).abs()

    override fun drawKeycap(batch: SpriteBatch) {
        // key background
        batch.color = keycapFill
        Toolkit.fillArea(batch, posX, posY, widthUp, heightUp)
        Toolkit.fillArea(batch, posX + posXgap - 1, posY + heightUp, widthDown + 1, 1)
        Toolkit.fillArea(batch, posX + posXgap, posY + heightUp + 1, widthDown, heightDown - 1)

        // key border
        batch.color = getBorderCol()
        val pack = CommonResourcePool.getAsTextureRegionPack("toolkit_box_border")
        val tx = pack.tileW.toFloat()
        val ty = pack.tileH.toFloat()

        // top edge
        batch.draw(pack.get(1, 0), posX.toFloat(), posY - ty, widthUp.toFloat(), ty)
        // bottom edge L
        batch.draw(pack.get(1, 2), posX.toFloat(), posY.toFloat() + heightUp, posXgap - 1f, ty)
        // bottom edge R
        batch.draw(pack.get(1, 2), posX.toFloat() + posXgap, posY.toFloat() + height, widthDown.toFloat(), ty)
        // left edge U
        batch.draw(pack.get(0, 1), posX.toFloat() - tx, posY.toFloat(), tx, heightUp.toFloat())
        // left edge D
        batch.draw(pack.get(0, 1), posX.toFloat() - tx + posXgap, posY + heightUp + 1f, tx, heightDown.toFloat())
        // right edge
        batch.draw(pack.get(2, 1), posX.toFloat() + width, posY.toFloat(), tx, height.toFloat())

        if (homerow) Toolkit.drawBoxBorder(batch, posX + 9, posY + 26, 10, 1)

    }

    override fun getKeysymPos() = (posX + posXgap + (minOf(widthUp, widthDown) - 20) / 2).toFloat() to posY + 20f
}
