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
        val TEXTINPUT_COL_GREY = Color.GRAY
        val TEXTINPUT_COL_BACKGROUND = Color(0x28282888)
        const val CURSOR_BLINK_TIME = 1f / 3f
    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width - 4, height - 4, true)

    var isActive = true
    var isGreyedOut = false

    var cursorX = 0 // 1 per code point
    var cursorDrawX = 0 // pixelwise point
    var cursorBlinkCounter = 0f
    var cursorOn = true

    val keybuf = StringBuilder()

    private var fboUpdateLatch = false

    override fun update(delta: Float) {
        super.update(delta)

        if (Terrarum.mouseDown) {
            //isActive = mouseUp
        }

        // process keypresses
        if (isActive) {
            IngameController.withKeyboardEvent { (_, char, _, keycodes) ->
                fboUpdateLatch = true

                if (keycodes.contains(Input.Keys.BACKSPACE) && cursorX > 0) {
                    cursorX -= 1
                    val charLen = Character.charCount(keybuf.codePointAt(cursorX))
                    keybuf.delete(keybuf.length - charLen, keybuf.length)
                    cursorDrawX = App.fontGame.getWidth("$keybuf")
                }
                // accept:
                // - literal "<"
                // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                else if (char != null && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                    keybuf.append(char)
                    cursorDrawX = App.fontGame.getWidth("$keybuf")
                    cursorX += 1
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

        batch.color = if (isActive) TEXTINPUT_COL_TEXT else TEXTINPUT_COL_GREY
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        batch.draw(fbo.colorBufferTexture, posX + 2f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())

        if (cursorOn) {
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