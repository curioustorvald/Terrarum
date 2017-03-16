package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

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
        val activeCol: Color = Color.white,
        val activeBackCol: Color = Color(0,0,0,0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Color(0x00f8ff),
        val highlightBackCol: Color = Color(0xb0b0b0),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = UIItemTextButton.defaultInactiveCol
) : UIItem(parentUI) {

    companion object {
        val font = Terrarum.fontGame!!
        val height = font.lineHeight * 2
        val defaultInactiveCol: Color = Color(0xc8c8c8)
    }

    private val label: String
        get() = if (readFromLang) Lang[labelText] else labelText


    override val height: Int = UIItemTextButton.height

    var highlighted: Boolean = false
    var mouseOver = false


    override fun update(gc: GameContainer, delta: Int) {
    }

    override fun render(gc: GameContainer, g: Graphics) {
        val textW = font.getWidth(label)


        if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode)
            g.color = highlightBackCol
            g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseOver) {
            BlendMode.resolve(activeBackBlendMode)
            g.color = activeBackCol
            g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }


        blendNormal()

        g.font = font
        mouseOver = mouseUp
        g.color = if (highlighted) highlightCol
                  else if (mouseOver)           activeCol
                  else                       inactiveCol

        g.drawString(label, posX.toFloat() + (width.minus(textW).div(2)), posY.toFloat() + height / 4)
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
