package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Text button. Height of hitbox is extended (double lineHeight, or 40 px) for better clicking
 *
 * Created by SKYHi14 on 2017-03-13.
 */
class UIItemTextButton(
        parentUI: UICanvas,
        val labelText: String,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        val readFromLang: Boolean = false,
        val activeCol: Color = Color.WHITE,
        val activeBackCol: Color = Color(0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Color(0x00f8ff_ff),
        val highlightBackCol: Color = Color(0xb0b0b0_ff.toInt()),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = UIItemTextButton.defaultInactiveCol,
        val preGapX:  Int = 0,
        val postGapX: Int = 0
) : UIItem(parentUI) {

    companion object {
        val font = TerrarumGDX.fontGame
        val height = font.lineHeight.toInt() * 2
        val defaultInactiveCol: Color = Color(0xc8c8c8_ff.toInt())
    }

    val label: String
        get() = if (readFromLang) Lang[labelText] else labelText


    override val height: Int = UIItemTextButton.height

    var highlighted: Boolean = false
    var mouseOver = false


    override fun update(delta: Float) {
    }

    private val glyphLayout = GlyphLayout()

    override fun render(batch: SpriteBatch) {
        val textW = font.getWidth(label)


        if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode)
            batch.color = highlightBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseOver) {
            BlendMode.resolve(activeBackBlendMode)
            batch.color = activeBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }

        blendNormal()


        batch.inUse {
            mouseOver = mouseUp
            batch.color = if (highlighted) highlightCol
            else if (mouseOver) activeCol
            else inactiveCol

            font.draw(batch,
                    label,
                    posX.toFloat() + width.minus(textW).div(2) + (preGapX - postGapX).div(2),
                    posY.toFloat() + height / 4
            )
        }
    }

    override fun keyPressed(key: Int, c: Char) {
    }

    override fun keyReleased(key: Int, c: Char) {
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
    }

    override fun mouseWheelMoved(change: Int) {
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
    }
}
