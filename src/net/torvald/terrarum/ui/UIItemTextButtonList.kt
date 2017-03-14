package net.torvald.terrarum.ui

import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.langpack.Lang
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class UIItemTextButtonList(
        parentUI: UICanvas,
        labelsList: Array<String>,
        override val width: Int,
        override val height: Int,
        val readFromLang: Boolean = false,

        // copied directly from UIItemTextButton
        activeCol: Color = Color.white,
        activeBackCol: Color = Color(0xd0d0d0),
        activeBackBlendMode: String = BlendMode.MULTIPLY,
        highlightCol: Color = Color(0x00f8ff),
        highlightBackCol: Color = Color(0xb0b0b0),
        highlightBackBlendMode: String = BlendMode.MULTIPLY,
        inactiveCol: Color = Color(0xc8c8c8)
) : UIItem(parentUI) {

    val buttons = labelsList.mapIndexed { index, s ->
        val height = this.height - UIItemTextButton.height
        UIItemTextButton(
                parentUI, s,
                posX = 0,
                posY = (height / labelsList.size.minus(1).toFloat() * index).roundInt(),
                width = width,
                readFromLang = true,
                activeCol = activeCol,
                activeBackCol = activeBackCol,
                activeBackBlendMode = activeBackBlendMode,
                highlightCol = highlightCol,
                highlightBackCol = highlightBackCol,
                highlightBackBlendMode = highlightBackBlendMode,
                inactiveCol = inactiveCol
        )
    }

    override var posX = 0
    override var posY = 0

    var selected: Int? = labelsList.size - 1 // default to "All"

    override fun update(gc: GameContainer, delta: Int) {
        buttons.forEachIndexed { index, btn ->
            // update width because Lang is mutable (you can change language at any moment)
            val textW = UIItemTextButton.font.getWidth(
                    if (readFromLang) Lang[btn.labelText] else btn.labelText
            )

            btn.update(gc, delta)


            if (btn.mousePushed) {
                selected = index
            }
            btn.highlighted = (index == selected) // forcibly highlight if this.highlighted != null

        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        buttons.forEach { it.render(gc, g) }
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