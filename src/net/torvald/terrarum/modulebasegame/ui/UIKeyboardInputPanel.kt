package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gamecontroller.IME
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.linearSearch
import net.torvald.terrarum.ui.*

/**
 * Created by minjaesong on 2021-11-10.
 */
class UIKeyboardInputPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 480
    override var height = 600
    override var openCloseTime = 0f


    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    internal val kbx = drawX + 1
    internal val kby = drawY + 95

    private val oneu = 28
    private val onehalfu = 44
    private val twou = 52
    private val twohalfu = 68
    private val threeu = 84
    private val spaceu = 188

    private val keycaps = hashMapOf(
            Input.Keys.GRAVE to UIItemInputKeycap(this, 1, 1, Input.Keys.GRAVE, oneu),
            Input.Keys.NUM_1 to UIItemInputKeycap(this, 33,1,  Input.Keys.NUM_1, oneu),
            Input.Keys.NUM_2 to UIItemInputKeycap(this, 65,1,  Input.Keys.NUM_2, oneu),
            Input.Keys.NUM_3 to UIItemInputKeycap(this, 97,1,  Input.Keys.NUM_3, oneu),
            Input.Keys.NUM_4 to UIItemInputKeycap(this, 129,1, Input.Keys.NUM_4, oneu),
            Input.Keys.NUM_5 to UIItemInputKeycap(this, 161,1, Input.Keys.NUM_5, oneu),
            Input.Keys.NUM_6 to UIItemInputKeycap(this, 193,1, Input.Keys.NUM_6, oneu),
            Input.Keys.NUM_7 to UIItemInputKeycap(this, 225,1, Input.Keys.NUM_7, oneu),
            Input.Keys.NUM_8 to UIItemInputKeycap(this, 257,1, Input.Keys.NUM_8, oneu),
            Input.Keys.NUM_9 to UIItemInputKeycap(this, 289,1, Input.Keys.NUM_9, oneu),
            Input.Keys.NUM_0 to UIItemInputKeycap(this, 321,1, Input.Keys.NUM_0, oneu),
            Input.Keys.MINUS to UIItemInputKeycap(this, 353,1, Input.Keys.MINUS, oneu),
            Input.Keys.EQUALS to UIItemInputKeycap(this, 385,1, Input.Keys.EQUALS, oneu),
            Input.Keys.BACKSPACE to UIItemInputKeycap(this, 417,1, Input.Keys.BACKSPACE, 60),

            Input.Keys.TAB to UIItemInputKeycap(this, 1,33,  Input.Keys.TAB, onehalfu),
            Input.Keys.Q to UIItemInputKeycap(this, 49,33,  Input.Keys.Q, oneu),
            Input.Keys.W to UIItemInputKeycap(this, 81,33,  Input.Keys.W, oneu),
            Input.Keys.E to UIItemInputKeycap(this, 113,33,  Input.Keys.E, oneu),
            Input.Keys.R to UIItemInputKeycap(this, 145,33,  Input.Keys.R, oneu),
            Input.Keys.T to UIItemInputKeycap(this, 177,33,  Input.Keys.T, oneu),
            Input.Keys.Y to UIItemInputKeycap(this, 209,33,  Input.Keys.Y, oneu),
            Input.Keys.U to UIItemInputKeycap(this, 241,33,  Input.Keys.U, oneu),
            Input.Keys.I to UIItemInputKeycap(this, 273,33,  Input.Keys.I, oneu),
            Input.Keys.O to UIItemInputKeycap(this, 305,33,  Input.Keys.O, oneu),
            Input.Keys.P to UIItemInputKeycap(this, 337,33,  Input.Keys.P, oneu),
            Input.Keys.LEFT_BRACKET to UIItemInputKeycap(this, 369,33,  Input.Keys.LEFT_BRACKET, oneu),
            Input.Keys.RIGHT_BRACKET to UIItemInputKeycap(this, 401,33,  Input.Keys.RIGHT_BRACKET, oneu),
            Input.Keys.BACKSLASH to UIItemInputKeycap(this, 433,33,  Input.Keys.BACKSLASH, onehalfu),

            Input.Keys.CAPS_LOCK to UIItemInputKeycap(this, 1,65,  Input.Keys.CAPS_LOCK, twou),
            Input.Keys.A to UIItemInputKeycap(this, 57,65,  Input.Keys.A, oneu),
            Input.Keys.S to UIItemInputKeycap(this, 89,65,  Input.Keys.S, oneu),
            Input.Keys.D to UIItemInputKeycap(this, 121,65,  Input.Keys.D, oneu),
            Input.Keys.F to UIItemInputKeycap(this, 153,65,  Input.Keys.F, oneu, true),
            Input.Keys.G to UIItemInputKeycap(this, 185,65,  Input.Keys.G, oneu),
            Input.Keys.H to UIItemInputKeycap(this, 217,65,  Input.Keys.H, oneu),
            Input.Keys.J to UIItemInputKeycap(this, 249,65,  Input.Keys.J, oneu, true),
            Input.Keys.K to UIItemInputKeycap(this, 281,65,  Input.Keys.K, oneu),
            Input.Keys.L to UIItemInputKeycap(this, 313,65,  Input.Keys.L, oneu),
            Input.Keys.SEMICOLON to UIItemInputKeycap(this, 345,65,  Input.Keys.SEMICOLON, oneu),
            Input.Keys.APOSTROPHE to UIItemInputKeycap(this, 377,65,  Input.Keys.APOSTROPHE, oneu),
            Input.Keys.ENTER to UIItemInputKeycap(this, 409,65,  Input.Keys.ENTER, twohalfu),

            Input.Keys.SHIFT_LEFT to UIItemInputKeycap(this, 1,97,  Input.Keys.SHIFT_LEFT, twohalfu),
            Input.Keys.Z to UIItemInputKeycap(this, 73,97,  Input.Keys.Z, oneu),
            Input.Keys.X to UIItemInputKeycap(this, 105,97,  Input.Keys.X, oneu),
            Input.Keys.C to UIItemInputKeycap(this, 137,97,  Input.Keys.C, oneu),
            Input.Keys.V to UIItemInputKeycap(this, 169,97,  Input.Keys.V, oneu),
            Input.Keys.B to UIItemInputKeycap(this, 201,97,  Input.Keys.B, oneu),
            Input.Keys.N to UIItemInputKeycap(this, 233,97,  Input.Keys.N, oneu),
            Input.Keys.M to UIItemInputKeycap(this, 265,97,  Input.Keys.M, oneu),
            Input.Keys.COMMA to UIItemInputKeycap(this, 297,97,  Input.Keys.COMMA, oneu),
            Input.Keys.PERIOD to UIItemInputKeycap(this, 329,97,  Input.Keys.PERIOD, oneu),
            Input.Keys.SLASH to UIItemInputKeycap(this, 361,97,  Input.Keys.SLASH, oneu),
            Input.Keys.SHIFT_RIGHT to UIItemInputKeycap(this, 393,97,  Input.Keys.SHIFT_RIGHT, threeu),

            Input.Keys.CONTROL_LEFT to UIItemInputKeycap(this, 1,129,  Input.Keys.CONTROL_LEFT, onehalfu),
            -2 to UIItemInputKeycap(this, 49,129,  null, oneu),
            Input.Keys.ALT_LEFT to UIItemInputKeycap(this, 81,129,  Input.Keys.ALT_LEFT, onehalfu),
            Input.Keys.SPACE to UIItemInputKeycap(this, 129,129,  Input.Keys.SPACE, spaceu),
            Input.Keys.ALT_RIGHT to UIItemInputKeycap(this, 321,129,  Input.Keys.ALT_RIGHT, onehalfu),
            -3 to UIItemInputKeycap(this, 369,129,  null, oneu),
            -4 to UIItemInputKeycap(this, 401,129,  null, oneu),
            Input.Keys.CONTROL_RIGHT to UIItemInputKeycap(this, 433,129,  Input.Keys.CONTROL_RIGHT, onehalfu),

            ) // end of keycaps

    private val textSelWidth = 266
    private val selectorWidth = 600
    private val halfselw = selectorWidth / 2
    private val selDrawX = (Toolkit.drawWidth - selectorWidth) / 2
    private val halfw = width / 2

    private val y1 = 400
    private val y2 = y1 + 40
    private val y3 = y2 + 48

    private val lowLayerCodes = IME.getAllLowLayers().sorted()
    private val lowLayerNames = lowLayerCodes.map { { IME.getLowLayerByName(it).name } }
    private val keyboardLayoutSelection = UIItemTextSelector(this,
            selDrawX + (halfselw - textSelWidth) / 2,
            y2,
            lowLayerNames,
            lowLayerCodes.linearSearch { it == App.getConfigString("basekeyboardlayout") } ?: throw IME.LayoutNotFound(App.getConfigString("basekeyboardlayout")),
            textSelWidth
    )

    private val imeCodes0 = IME.getAllHighLayers().sorted()
    private val imeCodes = listOf("none") + imeCodes0
    private val imeNames = listOf({"$EMDASH"}) + imeCodes0.map { { IME.getHighLayerByName(it).name } }
    private val imeSelection = UIItemTextSelector(this,
            selDrawX + halfselw + (halfselw - textSelWidth) / 2,
            y2,
            imeNames,
            imeCodes.linearSearch { it == App.getConfigString("inputmethod") } ?: throw IME.LayoutNotFound(App.getConfigString("inputmethod")),
            textSelWidth
    )



    private val keyboardTestPanel = UIItemTextLineInput(this,
            drawX + (width - 480) / 2 + 3,
            y3,
            474
    )


    init {
        keycaps.values.forEach { addUIitem(it) }


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

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        
        val txt1 = Lang["MENU_LABEL_KEYBOARD_LAYOUT"]; val tw1 = App.fontGame.getWidth(txt1)
        App.fontGame.draw(batch, txt1, selDrawX + (halfselw - tw1) / 2, y1)

        val txt2 = Lang["MENU_LABEL_IME"]; val tw2 = App.fontGame.getWidth(txt2)
        App.fontGame.draw(batch, txt2, selDrawX + halfselw + (halfselw - tw2) / 2, y1)

        // title
        val title = Lang["MENU_CONTROLS_KEYBOARD"]
        batch.color = Color.WHITE
        App.fontGame.draw(batch, title, drawX.toFloat() + (width - App.fontGame.getWidth(title)) / 2, drawY.toFloat())

        uiItems.forEach { it.render(batch, camera) }
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
private class UIItemInputKeycap(
        parent: UICanvas,
        initialX: Int,
        initialY: Int,
        val key: Int?,
        override val width: Int,
        val homerow: Boolean = false
) : UIItem(parent, initialX, initialY) {

    init {
        if (parent is UIKeyboardControlPanel ) {
            this.posX = initialX + parent.kbx
            this.posY = initialY + parent.kby
        }
        else if (parent is UIKeyboardInputPanel) {
            this.posX = initialX + parent.kbx
            this.posY = initialY + parent.kby
        }
    }

    override val height = 28

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    var selected = false

    private val borderKeyForbidden = Color(0x000000C0)
    private val borderKeyNormal = Toolkit.Theme.COL_INACTIVE
    private val borderMouseUp = Toolkit.Theme.COL_ACTIVE
    private val borderKeyPressed = Toolkit.Theme.COL_HIGHLIGHT
    private val borderKeyPressedAndSelected = Color(0x33FF33FF.toInt())

    private val keycapFill = Toolkit.Theme.COL_CELL_FILL

    private val keylabelCol = Toolkit.Theme.COL_DISABLED
    private val configuredKeyCol = Color.WHITE

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        // key background
        batch.color = keycapFill
        Toolkit.fillArea(batch, posX, posY, width, height)

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


        // keysym
        if (key == Input.Keys.CONTROL_LEFT || key == Input.Keys.CONTROL_RIGHT)
            batch.draw(labels.get(21,3), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.ALT_LEFT || key == Input.Keys.ALT_RIGHT)
            batch.draw(labels.get(22,3), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.SHIFT_LEFT || key == Input.Keys.SHIFT_RIGHT)
            batch.draw(labels.get(23,3), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.TAB)
            batch.draw(labels.get(23,5), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.BACKSPACE)
            batch.draw(labels.get(24,5), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.CAPS_LOCK)
            batch.draw(labels.get(24,3), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else if (key == Input.Keys.ENTER)
            batch.draw(labels.get(17,3), (posX + (width - 20) / 2).toFloat(), posY + 4f)
        else {
            // the real keysym
        }

    }

    override fun dispose() {
    }
}