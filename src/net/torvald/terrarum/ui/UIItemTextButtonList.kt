package net.torvald.terrarum.ui

import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Millisec
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
        val activeCol: Color = Color.white,
        val activeBackCol: Color = Color(0,0,0,0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Color(0x00f8ff),
        val highlightBackCol: Color = Color(0xb0b0b0),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = Color(0xc8c8c8),
        val backgroundCol: Color = Color(0,0,0,0),
        val backgroundBlendMode: String = BlendMode.NORMAL,
        val kinematic: Boolean = false // more "kinetic" movement of selector
) : UIItem(parentUI) {

    val buttons = labelsList.mapIndexed { index, s ->
        val height = this.height - UIItemTextButton.height
        if (!kinematic) {
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
        else {
            UIItemTextButton(
                    parentUI, s,
                    posX = 0,
                    posY = (height / labelsList.size.minus(1).toFloat() * index).roundInt(),
                    width = width,
                    readFromLang = true,
                    activeBackCol = Color(0,0,0,0),
                    activeBackBlendMode = BlendMode.NORMAL,
                    highlightBackCol = Color(0,0,0,0)
            )
        }
    }

    override var posX = 0
    override var posY = 0

    var selected = labelsList.size - 1 // default to "All"
    private var highlightY = buttons[selected].posY.toDouble()
    private val highlighterMoveDuration: Millisec = 100
    private var highlighterMoveTimer: Millisec = 0
    private var highlighterMoving = false
    private var highlighterYStart = highlightY
    private var highlighterYEnd = highlightY


    override fun update(gc: GameContainer, delta: Int) {
        if (highlighterMoving) {
            highlighterMoveTimer += delta
            highlightY = UIUtils.moveQuick(
                    highlighterYStart,
                    highlighterYEnd,
                    highlighterMoveTimer.toDouble(),
                    highlighterMoveDuration.toDouble()
            )

            if (highlighterMoveTimer > highlighterMoveDuration) {
                highlighterMoveTimer = 0
                highlighterYStart = highlighterYEnd
                highlightY = highlighterYEnd
                highlighterMoving = false
            }
        }

        buttons.forEachIndexed { index, btn ->
            btn.update(gc, delta)


            if (btn.mousePushed && index != selected) {
                if (kinematic) {
                    highlighterYStart = buttons[selected].posY.toDouble()
                    selected = index
                    highlighterMoving = true
                    highlighterYEnd = buttons[selected].posY.toDouble()
                }
                else {
                    selected = index
                    highlightY = buttons[selected].posY.toDouble()
                }
            }
            btn.highlighted = (index == selected) // forcibly highlight if this.highlighted != null

        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.color = backgroundCol
        BlendMode.resolve(backgroundBlendMode)
        g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

        g.color = highlightBackCol
        g.fillRect(posX.toFloat(), highlightY.toFloat(), width.toFloat(), UIItemTextButton.height.toFloat())

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