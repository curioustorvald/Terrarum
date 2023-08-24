package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gamecontroller.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.Ascii85Codec
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.utils.Clipboard

/**
 * Created by minjaesong on 2023-08-24.
 */
class UIImportAvatar(val remoCon: UIRemoCon) : Advanceable() {

    override var width = 480 // SAVE_CELL_WIDTH
    override var height = 480
    override var openCloseTime: Second = OPENCLOSE_GENERIC

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2
    private val cols = 80
    private val rows = 30
    private val goButtonWidth = 180


    private val codeBox = UIItemCodeBox(this, (Toolkit.drawWidth - App.fontSmallNumbers.W * cols) / 2, drawY, cols, rows)

    private val clearButton = UIItemTextButton(this,
        { Lang["MENU_IO_CLEAR"] }, drawX + (width/2 - goButtonWidth) / 2, drawY + height - 24 - 34, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val pasteButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_PASTE"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, drawY + height - 24 - 34, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)


    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, drawX + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val goButton = UIItemTextButton(this,
        { Lang["MENU_IO_IMPORT"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

    init {
        addUIitem(codeBox)
        addUIitem(clearButton)
        addUIitem(pasteButton)
        addUIitem(backButton)
        addUIitem(goButton)

        clearButton.clickOnceListener = { _,_ ->
            codeBox.clearTextBuffer()
        }
        pasteButton.clickOnceListener = { _,_ ->
            codeBox.pasteFromClipboard()
        }
        backButton.clickOnceListener = { _,_ ->
            remoCon.openUI(UILoadSavegame(remoCon))
        }
        goButton.clickOnceListener = { _,_ ->
            doImport()
        }
    }


    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }

    override fun advanceMode(button: UIItem) {
    }

    private fun doImport() {
        val rawStr = codeBox.textBuffer.toString()
        // sanity check


        val ascii85codec = Ascii85Codec((33..117).map { it.toChar() }.joinToString(""))
        val ascii85str = rawStr.substring(2 until rawStr.length - 2).replace("z", "!!!!!")
    }
}


class UIItemCodeBox(parent: UIImportAvatar, initialX: Int, initialY: Int, val cols: Int = 80, val rows: Int = 24) : UIItem(parent, initialX, initialY) {

    override val width = App.fontSmallNumbers.W * cols
    override val height = App.fontSmallNumbers.H * rows
    override fun dispose() {
    }

    private var highlightCol: Color = Toolkit.Theme.COL_INACTIVE

    var selected = false

    private var textCursorPosition = 0
    internal var textBuffer = StringBuilder()

    fun pasteFromClipboard() {
        textBuffer.clear()
        Clipboard.fetch().forEach {
            if (it.code in 33..122) textBuffer.append(it)
        }
        textCursorPosition += textBuffer.length
    }

    override fun update(delta: Float) {
        super.update(delta)
//        if (!selected && mousePushed)
//            selected = true
//        else if (selected && mouseDown && !mouseUp)
//            selected = false

        if (textBuffer.isEmpty() && (Gdx.input.isKeyJustPressed(Input.Keys.V) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)))) {
            pasteFromClipboard()
        }
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        // draw box backgrounds
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, posX, posY, width, height)

        // draw borders
        batch.color = if (selected) Toolkit.Theme.COL_SELECTED else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)

        // draw texts
        if (textBuffer.isEmpty()) {
            batch.color = Toolkit.Theme.COL_INACTIVE
            App.fontGame.draw(batch, Lang["CONTEXT_IMPORT_AVATAR_INSTRUCTION_1"], posX + 5, posY)
            App.fontGame.draw(batch, Lang["CONTEXT_IMPORT_AVATAR_INSTRUCTION_2"], posX + 5f, posY + App.fontGame.lineHeight)
        }
        else {
            batch.color = Color.WHITE
            val scroll = ((textBuffer.length.toDouble() / cols).ceilToInt() - rows).coerceAtLeast(0)
            for (i in scroll * cols until textBuffer.length) {
                val c = textBuffer[i]
                val x = ((i - scroll * cols) % cols) * App.fontSmallNumbers.W.toFloat()
                val y = ((i - scroll * cols) / cols) * App.fontSmallNumbers.H.toFloat()

                App.fontSmallNumbers.draw(batch, "$c", posX + x, posY + y)
            }
        }
    }

    fun clearTextBuffer() {
        textBuffer.clear()
        textCursorPosition = 0
    }
}