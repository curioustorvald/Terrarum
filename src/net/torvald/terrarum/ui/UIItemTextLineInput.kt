package net.torvald.terrarum.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.utils.Clipboard
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.streams.toList

data class InputLenCap(val count: Int, val unit: CharLenUnit) {
    enum class CharLenUnit {
        UTF8_BYTES, UTF16_CHARS, CODEPOINTS
    }

    fun exceeds(codepoints: CodepointSequence, extra: List<Int> = CodepointSequence()): Boolean {
        return when (unit) {
            CharLenUnit.CODEPOINTS -> (codepoints.size + extra.size) > count
            CharLenUnit.UTF16_CHARS -> {
                var cnt = 0
                listOf(codepoints,extra).forEach { it.forEach {
                    cnt += 1 + (it > 65535).toInt()
                } }
                cnt > count
            }
            CharLenUnit.UTF8_BYTES -> {
                var cnt = 0
                listOf(codepoints,extra).forEach { it.forEach {
                    cnt += if (it > 65535) 4 else if (it > 2047) 3 else if (it > 127) 2 else 1
                } }
                cnt > count
            }
        }
    }
}

/**
 * @param width width of the text input where the text gets drawn, not the entire item
 * @param height height of the text input where the text gets drawn, not the entire item
 *
 * Created by minjaesong on 2021-10-20.
 */
class UIItemTextLineInput(
        parentUI: UICanvas,
        initialX: Int, initialY: Int,
        override val width: Int,
        var placeholder: () -> String = { "" },
        val enablePasteButton: Boolean = true,
        val enableIMEButton: Boolean = false,
        val maxLen: InputLenCap = InputLenCap(1000, InputLenCap.CharLenUnit.CODEPOINTS)
) : UIItem(parentUI, initialX, initialY) {

    init {
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24

    private val buttonsShown = enableIMEButton.toInt() + enablePasteButton.toInt()

    companion object {
        val TEXTINPUT_COL_TEXT = Color.WHITE
        val TEXTINPUT_COL_TEXT_NOMORE = Color(0xFF8888FF.toInt())
        val TEXTINPUT_COL_TEXT_DISABLED = Toolkit.Theme.COL_DISABLED
        val TEXTINPUT_COL_BACKGROUND = Toolkit.Theme.COL_CELL_FILL
        const val CURSOR_BLINK_TIME = 1f / 3f

        private const val UI_TEXT_MARGIN = 2
        private const val WIDTH_ONEBUTTON = 24
    }

    private val fbo = FrameBuffer(
            Pixmap.Format.RGBA8888,
            width - 2 * UI_TEXT_MARGIN - buttonsShown * (WIDTH_ONEBUTTON + 3),
            height - 2 * UI_TEXT_MARGIN,
            true
    )

    var isActive = true

    var cursorX = 0
    var cursorDrawScroll = 0
    var cursorDrawX = 0 // pixelwise point
    var cursorBlinkCounter = 0f
    var cursorOn = true

    private val textbuf = CodepointSequence()

    private var fboUpdateLatch = true

    private var currentPlaceholderText = ArrayList<Int>(placeholder().toCodePoints()) // the placeholder text may change every time you call it


    private val btn1PosX = posX + width - 2*WIDTH_ONEBUTTON - 3
    private val btn2PosX = posX + width - WIDTH_ONEBUTTON

    private val mouseUpOnTextArea: Boolean
        get() = relativeMouseX in 0 until fbo.width + 2* UI_TEXT_MARGIN && relativeMouseY in 0 until height
    private val mouseUpOnButton1
        get() = buttonsShown > 1 && relativeMouseX in btn1PosX - posX until btn1PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height
    private val mouseUpOnButton2
        get() = buttonsShown > 0 && relativeMouseX in btn2PosX - posX until btn2PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height

    private var imeOn = false

    private fun forceLitCursor() {
        cursorBlinkCounter = 0f
        cursorOn = true
    }

    private fun tryCursorBack() {
        if (cursorDrawX > fbo.width) {
            val d = cursorDrawX - fbo.width
            cursorDrawScroll = d
        }
    }
    private fun tryCursorForward() {
        if (cursorDrawX - cursorDrawScroll < 0) {
            cursorDrawScroll -= cursorDrawX
        }
    }

    override fun update(delta: Float) {
        super.update(delta)
        val mouseDown = Terrarum.mouseDown

        if (mouseDown) {
            isActive = mouseUp
        }

        // TODO cursorDrawX kerning-aware

        // process keypresses
        if (isActive) {
            IngameController.withKeyboardEvent { (_, char, _, keycodes) ->
                fboUpdateLatch = true
                forceLitCursor()

                if (keycodes.contains(Input.Keys.V) && (keycodes.contains(Input.Keys.CONTROL_LEFT) || keycodes.contains(Input.Keys.CONTROL_RIGHT))) {
                    paste()
                    tryCursorBack()
                }
                else if (keycodes.contains(Input.Keys.C) && (keycodes.contains(Input.Keys.CONTROL_LEFT) || keycodes.contains(Input.Keys.CONTROL_RIGHT))) {
                    copyToClipboard()
                }
                else if (cursorX > 0 && keycodes.contains(Input.Keys.BACKSPACE)) {
                    cursorX -= 1
                    textbuf.removeAt(cursorX)
                    cursorDrawX = App.fontGame.getWidth(textbuf.subList(0, cursorX))
                    tryCursorForward()
                }
                else if (cursorX > 0 && keycodes.contains(Input.Keys.LEFT)) {
                    cursorX -= 1
                    cursorDrawX = App.fontGame.getWidth(textbuf.subList(0, cursorX))
                    tryCursorForward()
                                 }
                else if (cursorX < textbuf.size && keycodes.contains(Input.Keys.RIGHT)) {
                    cursorX += 1
                    cursorDrawX = App.fontGame.getWidth(textbuf.subList(0, cursorX))
                    tryCursorBack()
                }
                // accept:
                // - literal "<"
                // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                else if (char != null && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                    val codepoints = char.toCodePoints()

                    if (!maxLen.exceeds(textbuf, codepoints)) {
                        textbuf.addAll(cursorX, codepoints)

                        cursorX += codepoints.size
                        cursorDrawX = App.fontGame.getWidth(textbuf.subList(0, cursorX))

                        tryCursorBack()
                    }
                }

                // don't put innards of tryCursorBack/Forward here -- you absolutely don't want that behaviour

            }

            if (textbuf.size == 0) {
                currentPlaceholderText = ArrayList(placeholder().toCodePoints())
            }

            cursorBlinkCounter += delta

            while (cursorBlinkCounter >= CURSOR_BLINK_TIME) {
                cursorBlinkCounter -= CURSOR_BLINK_TIME
                cursorOn = !cursorOn
            }
        }

        if (mouseDown && !mouseLatched && (enablePasteButton && enableIMEButton && mouseUpOnButton1 || enableIMEButton && !enablePasteButton && mouseUpOnButton2)) {
            toggleIME()
            mouseLatched = true
        }
        else if (mouseDown && !mouseLatched && (enablePasteButton && enableIMEButton && mouseUpOnButton2 || enablePasteButton && !enableIMEButton && mouseUpOnButton2)) {
            paste()
            mouseLatched = true
        }

        if (!mouseDown) mouseLatched = false
    }

    private fun String.toCodePoints() = this.codePoints().toList()

    private fun toggleIME() {
        imeOn = !imeOn
    }

    private fun paste() {
        val codepoints = Clipboard.fetch().substringBefore('\n').substringBefore('\t').toCodePoints()

        val actuallyInserted = arrayListOf(0)

        for (c in codepoints) {
            if (maxLen.exceeds(textbuf, actuallyInserted)) break
            actuallyInserted.add(c)
        }

        actuallyInserted.removeAt(0)

        textbuf.addAll(cursorX, actuallyInserted)

        cursorX += actuallyInserted.size
        cursorDrawX = App.fontGame.getWidth(textbuf.subList(0, cursorX))

        tryCursorBack()

        fboUpdateLatch = true
    }

    private fun copyToClipboard() {
        Clipboard.copy(textbufToString())
    }

    private fun textbufToString(): String {
        return ""
    }

    override fun render(batch: SpriteBatch, camera: Camera) {

        batch.end()

        if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                App.fontGameFBO.draw(it, if (textbuf.isEmpty()) currentPlaceholderText else textbuf, -1f*cursorDrawScroll, 0f)
            } }
        }

        batch.begin()

        val mouseDown = Terrarum.mouseDown

        // text area cell back
        batch.color = TEXTINPUT_COL_BACKGROUND
        Toolkit.fillArea(batch, posX, posY, fbo.width + 2 * UI_TEXT_MARGIN, height)
        // rightmost button cell back
        if (buttonsShown > 0)
            Toolkit.fillArea(batch, btn2PosX, posY, WIDTH_ONEBUTTON, height)
        if (buttonsShown > 1)
            Toolkit.fillArea(batch, btn1PosX, posY, WIDTH_ONEBUTTON, height)

        // text area border (base)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        if (buttonsShown > 0)
            Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        if (buttonsShown > 1)
            Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)

        // text area border (pop-up for isActive)
        if (isActive) {
            batch.color = Toolkit.Theme.COL_HIGHLIGHT
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        }

        // button border
        if (mouseUpOnButton2) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnButton1) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnTextArea && !isActive) {
            batch.color = Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, fbo.width + 2 * UI_TEXT_MARGIN+ 2, height + 2)
        }



        // draw text
        batch.color = if (textbuf.isEmpty()) TEXTINPUT_COL_TEXT_DISABLED else TEXTINPUT_COL_TEXT
        batch.draw(fbo.colorBufferTexture, posX + 2f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())

        // draw text cursor
        if (isActive && cursorOn) {
            val baseCol = if (maxLen.exceeds(textbuf, listOf(32))) TEXTINPUT_COL_TEXT_NOMORE else TEXTINPUT_COL_TEXT

            batch.color = baseCol.cpy().mul(0.5f,0.5f,0.5f,1f)
            Toolkit.fillArea(batch, posX - cursorDrawScroll + cursorDrawX + 3, posY, 2, 24)

            batch.color = baseCol
            Toolkit.fillArea(batch, posX - cursorDrawScroll + cursorDrawX + 3, posY, 1, 23)
        }

        // draw icon
        if (enablePasteButton && enableIMEButton) {
            // IME button
            batch.color = if (mouseUpOnButton1 && mouseDown || imeOn) Toolkit.Theme.COL_HIGHLIGHT else if (mouseUpOnButton1) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(7,2), btn1PosX + 2f, posY + 2f)
            // paste button
            batch.color = if (mouseUpOnButton2 && mouseDown) Toolkit.Theme.COL_HIGHLIGHT else if (mouseUpOnButton2) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(8,2), btn2PosX + 2f, posY + 2f)
        }
        else if (!enableIMEButton && enablePasteButton) {
            // paste button
            batch.color = if (mouseUpOnButton2 && mouseDown) Toolkit.Theme.COL_HIGHLIGHT else if (mouseUpOnButton2) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(8,2), btn2PosX + 2f, posY + 2f)
        }
        else if (!enablePasteButton && enableIMEButton) {
            // IME button
            batch.color = if (mouseUpOnButton1 && mouseDown || imeOn) Toolkit.Theme.COL_HIGHLIGHT else if (mouseUpOnButton1) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(7,2), btn2PosX + 2f, posY + 2f)
        }


        super.render(batch, camera)
    }

    fun getText() = textbufToString()
    fun getTextOrPlaceholder() = if (textbuf.isEmpty()) currentPlaceholderText else getText()

    override fun dispose() {
        fbo.dispose()
    }


}