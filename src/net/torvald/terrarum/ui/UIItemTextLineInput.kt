package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import java.awt.Color

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
        override val height: Int,
        var placeholder: String? = null,
        val enablePasteButton: Boolean = true,
        val enableLanguageButton: Boolean = false
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        val TEXTINPUT_COL_TEXT = Color.WHITE
        val TEXTINPUT_COL_GREY = Color.GRAY

    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)

    var isActive = true
    var isGreyedOut = false

    val cursorX = 0
    val keybuf = StringBuilder()


    override fun update(delta: Float) {
        super.update(delta)

        if (Terrarum.mouseDown) {
            isActive = mouseUp
        }

        // process keypresses
        if (isActive) {

        }
    }


    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)
    }

    override fun dispose() {
        fbo.dispose()
    }


}