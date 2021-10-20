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
        override val height: Int = 24,
        var placeholder: String? = null,
        val enablePasteButton: Boolean = true,
        val enableLanguageButton: Boolean = false
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        val TEXTINPUT_COL_TEXT = Color.WHITE
        val TEXTINPUT_COL_BORDER = UIItemTextButton.defaultActiveCol
        val TEXTINPUT_COL_BORDER_INACTIVE = Color.LIGHT_GRAY
        val TEXTINPUT_COL_BACKGROUND = Color(0x28282888)
        const val CURSOR_BLINK_TIME = 1f / 3f
    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width - 4, height - 4, true)

    var isActive = true
    var isGreyedOut = false

    var cursorX = 0 // 1 per char (not codepoint)
    var cursorCodepoint = 0
    var codepointCount = 0
    var cursorDrawX = 0 // pixelwise point
    var cursorBlinkCounter = 0f
    var cursorOn = true

    val keybuf = StringBuilder()

    private var fboUpdateLatch = false

    override fun update(delta: Float) {
        super.update(delta)

        if (Terrarum.mouseDown) {
            isActive = mouseUp
        }

        // process keypresses
        if (isActive) {
            IngameController.withKeyboardEvent { (_, char, _, keycodes) ->
                fboUpdateLatch = true

                if (cursorX > 0 && keycodes.contains(Input.Keys.BACKSPACE)) {
                    cursorCodepoint -= 1
                    val lastCp = keybuf.codePointAt(cursorCodepoint)
                    val charCount = Character.charCount(lastCp)
                    cursorX -= charCount
                    keybuf.delete(cursorX, cursorX + charCount)

                    cursorDrawX -= App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                    codepointCount -= 1
                }
                else if (cursorX > 0 && keycodes.contains(Input.Keys.LEFT)) {
                    cursorCodepoint -= 1
                    cursorX -= Character.charCount(keybuf.codePointAt(cursorCodepoint))
                    val lastCp = keybuf.codePointAt(cursorCodepoint)
                    cursorDrawX -= App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                }
                else if (cursorX < codepointCount && keycodes.contains(Input.Keys.RIGHT)) {
                    val lastCp = keybuf.codePointAt(cursorCodepoint)
                    cursorDrawX += App.fontGame.getWidth(String(Character.toChars(lastCp))) - 1
                    cursorX += Character.charCount(lastCp)
                    cursorCodepoint += 1
                }
                // accept:
                // - literal "<"
                // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                else if (char != null && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                    keybuf.insert(cursorX, char)

                    cursorDrawX += App.fontGame.getWidth(char) - 1
                    cursorX += char.length
                    codepointCount += 1
                    cursorCodepoint += 1
                }
            }

            cursorBlinkCounter += delta

            while (cursorBlinkCounter >= CURSOR_BLINK_TIME) {
                cursorBlinkCounter -= CURSOR_BLINK_TIME
                cursorOn = !cursorOn
            }
        }
    }


    override fun render(batch: SpriteBatch, camera: Camera) {

        batch.end()

        if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                App.fontGame.draw(it, "$keybuf", 0f, 0f)
            } }
        }

        batch.begin()

        batch.color = TEXTINPUT_COL_BACKGROUND
        Toolkit.fillArea(batch, posX, posY, width, height)

        batch.color = if (isActive) TEXTINPUT_COL_BORDER else TEXTINPUT_COL_BORDER_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)

        batch.color = TEXTINPUT_COL_TEXT
        batch.draw(fbo.colorBufferTexture, posX + 2f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())

        if (isActive && cursorOn) {
            val oldBatchCol = batch.color.cpy()

            batch.color = batch.color.mul(0.5f,0.5f,0.5f,1f)
            Toolkit.fillArea(batch, posX + cursorDrawX + 3, posY, 2, 24)

            batch.color = oldBatchCol
            Toolkit.fillArea(batch, posX + cursorDrawX + 3, posY, 1, 23)
        }


        super.render(batch, camera)
    }

    override fun dispose() {
        fbo.dispose()
    }


}