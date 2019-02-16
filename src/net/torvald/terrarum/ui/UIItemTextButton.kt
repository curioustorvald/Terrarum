package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.langpack.Lang

/**
 * Text button. Height of hitbox is extended (double lineHeight, or 40 px) for better clicking
 *
 * Created by minjaesong on 2017-03-13.
 */
open class UIItemTextButton(
        parentUI: UICanvas,
        /** Stored text (independent to the Langpack) */
        val labelText: String,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        val readFromLang: Boolean = false,

        /** Colour when mouse is over */
        val activeCol: Color = UIItemTextButton.defaultActiveCol,
        /** Colour when mouse is over */
        val activeBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = UIItemTextButton.defaultHighlightCol,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = UIItemTextButton.defaultInactiveCol,
        val backgroundCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,


        val preGapX:  Int = 0,
        val postGapX: Int = 0,

        val alignment: Alignment = Alignment.CENTRE,
        val hitboxSize: Int = UIItemTextButton.height
) : UIItem(parentUI) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    companion object {
        val font = Terrarum.fontGame
        val height = font.lineHeight.toInt()
        val defaultInactiveCol = Color.WHITE
        val defaultHighlightCol = Color(0x00f8ff_ff)
        val defaultActiveCol = Color(0xfff066_ff.toInt())

        enum class Alignment {
            CENTRE, LEFT, RIGHT
        }
    }

    /** Actually displayed text (changes with the app language) */
    val label: String
        get() = if (readFromLang) Lang[labelText] else labelText


    override val height: Int = hitboxSize

    var highlighted: Boolean = false

    override fun render(batch: SpriteBatch, camera: Camera) {
        val textW = font.getWidth(label)


        // draw background
        if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode, batch)
            batch.color = highlightBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseUp) {
            BlendMode.resolve(activeBackBlendMode, batch)
            batch.color = activeBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }


        blendNormal(batch)


        batch.color = if (highlighted) highlightCol
        else if (mouseUp) activeCol
        else inactiveCol

        font.draw(batch,
                label,
                when (alignment) {
                    Alignment.CENTRE -> posX.toFloat() + width.minus(textW).div(2) + (preGapX - postGapX).div(2)
                    Alignment.LEFT -> posX.toFloat() + preGapX
                    Alignment.RIGHT -> width - postGapX - textW.toFloat()
                },
                posY.toFloat() + (hitboxSize - UIItemTextButton.height) / 2f
        )
    }

    override fun dispose() {
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }
}
