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
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

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
        val enableIMEButton: Boolean = false
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
        val TEXTINPUT_COL_TEXT_HALF = Color.WHITE.cpy().mul(1f,1f,1f,0.5f)
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

    var cursorX = 0 // 1 per char (not codepoint)
    var cursorCodepoint = 0
    var codepointCount = 0
    var cursorDrawX = 0 // pixelwise point
    var cursorBlinkCounter = 0f
    var cursorOn = true

    private val textbuf = StringBuilder()

    private var fboUpdateLatch = true

    private var currentPlaceholderText = placeholder() // the placeholder text may change every time you call it


    private val btn1PosX = posX + width - 2*WIDTH_ONEBUTTON - 3
    private val btn2PosX = posX + width - WIDTH_ONEBUTTON

    private val mouseUpOnTextArea: Boolean
        get() = relativeMouseX in 0 until fbo.width + 2* UI_TEXT_MARGIN && relativeMouseY in 0 until height
    private val mouseUpOnButton1
        get() = buttonsShown > 1 && relativeMouseX in btn1PosX - posX until btn1PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height
    private val mouseUpOnButton2
        get() = buttonsShown > 0 && relativeMouseX in btn2PosX - posX until btn2PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height

    private var imeOn = false

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

                if (keycodes.contains(Input.Keys.V) && (keycodes.contains(Input.Keys.CONTROL_LEFT) || keycodes.contains(Input.Keys.CONTROL_RIGHT))) {
                    paste()
                }
                else if (keycodes.contains(Input.Keys.C) && (keycodes.contains(Input.Keys.CONTROL_LEFT) || keycodes.contains(Input.Keys.CONTROL_RIGHT))) {
                    copyToClipboard()
                }
                else if (cursorX > 0 && keycodes.contains(Input.Keys.BACKSPACE)) {
                    cursorCodepoint -= 1
                    val lastCp = textbuf.codePointAt(cursorCodepoint)
                    val charCount = Character.charCount(lastCp)
                    cursorX -= charCount
                    textbuf.delete(cursorX, cursorX + charCount)

                    cursorDrawX -= App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                    codepointCount -= 1
                }
                else if (cursorX > 0 && keycodes.contains(Input.Keys.LEFT)) {
                    cursorCodepoint -= 1
                    cursorX -= Character.charCount(textbuf.codePointAt(cursorCodepoint))
                    val lastCp = textbuf.codePointAt(cursorCodepoint)
                    cursorDrawX -= App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                    if (cursorDrawX < 0) cursorDrawX = 0
                }
                else if (cursorX < codepointCount && keycodes.contains(Input.Keys.RIGHT)) {
                    val lastCp = textbuf.codePointAt(cursorCodepoint)
                    cursorDrawX += App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                    cursorX += Character.charCount(lastCp)
                    cursorCodepoint += 1
                }
                // accept:
                // - literal "<"
                // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                else if (char != null && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                    textbuf.insert(cursorX, char)

                    cursorDrawX += App.fontGame.getWidth(char) - 1
                    cursorX += char.length
                    codepointCount += 1
                    cursorCodepoint += 1
                }
            }

            if (cursorCodepoint == 0) {
                currentPlaceholderText = placeholder()
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

    private fun toggleIME() {
        imeOn = !imeOn
    }

    private fun paste() {
        val str = Clipboard.fetch().substringBefore('\n').substringBefore('\t')
        val strCodepointLen = str.codePoints().count().toInt()

        textbuf.insert(cursorX, str)

        cursorDrawX += App.fontGame.getWidth(str) - 1
        cursorX += str.length

        codepointCount += strCodepointLen
        cursorCodepoint += strCodepointLen

        fboUpdateLatch = true
    }

    private fun copyToClipboard() {
        Clipboard.copy(textbuf.toString())
    }

    override fun render(batch: SpriteBatch, camera: Camera) {

        batch.end()

        if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                App.fontGameFBO.draw(it, if (textbuf.isEmpty()) currentPlaceholderText else "$textbuf", 0f, 0f)
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
            batch.color = TEXTINPUT_COL_TEXT_HALF
            Toolkit.fillArea(batch, posX + cursorDrawX + 3, posY, 2, 24)

            batch.color = TEXTINPUT_COL_TEXT
            Toolkit.fillArea(batch, posX + cursorDrawX + 3, posY, 1, 23)
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

    fun getText() = textbuf.toString()
    fun getTextOrPlaceholder() = if (textbuf.isEmpty()) currentPlaceholderText else getText()

    override fun dispose() {
        fbo.dispose()
    }


}