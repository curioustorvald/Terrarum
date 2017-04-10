package net.torvald.terrarum.ui

import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SpriteSheet

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class UIItemTextButtonList(
        parentUI: UICanvas,
        labelsList: Array<String>,
        override val width: Int,
        override val height: Int,
        val verticalGutter: Int = 0,
        val readFromLang: Boolean = false,
        val defaultSelection: Int = 0,

        // icons
        val textAreaWidth: Int,
        val iconSpriteSheet: SpriteSheet? = null,
        val iconSpriteSheetIndices: IntArray? = null,

        // copied directly from UIItemTextButton
        val activeCol: Color = Color(0xfff066),
        val activeBackCol: Color = Color(0,0,0,0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Color(0x00f8ff),
        val highlightBackCol: Color = Color(0xb0b0b0),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = Color(0xc0c0c0),
        val backgroundCol: Color = Color(0,0,0,0),
        val backgroundBlendMode: String = BlendMode.NORMAL,
        val kinematic: Boolean = false // more "kinetic" movement of selector
) : UIItem(parentUI) {

    val iconToTextGap = 20
    val iconCellWidth = (iconSpriteSheet?.width ?: -iconToTextGap) / (iconSpriteSheet?.horizontalCount ?: 1)
    val iconCellHeight = (iconSpriteSheet?.height ?: 0) / (iconSpriteSheet?.verticalCount ?: 1)

    // zero if iconSpriteSheet is null
    val iconsWithGap: Int = iconToTextGap + iconCellWidth
    val pregap = (width - textAreaWidth - iconsWithGap) / 2 + iconsWithGap
    val postgap = (width - textAreaWidth - iconsWithGap) / 2

    val buttons = labelsList.mapIndexed { index, s ->
        val height = this.height - UIItemTextButton.height
        if (!kinematic) {
            UIItemTextButton(
                    parentUI, s,
                    posX = 0,
                    posY = verticalGutter + ((height - 2 * verticalGutter) / labelsList.size.minus(1).toFloat() * index).roundInt(),
                    width = width,
                    readFromLang = readFromLang,
                    activeCol = activeCol,
                    activeBackCol = activeBackCol,
                    activeBackBlendMode = activeBackBlendMode,
                    highlightCol = highlightCol,
                    highlightBackCol = highlightBackCol,
                    highlightBackBlendMode = highlightBackBlendMode,
                    inactiveCol = inactiveCol,
                    preGapX = pregap,
                    postGapX = postgap
            )
        }
        else {
            UIItemTextButton(
                    parentUI, s,
                    posX = 0,
                    posY = verticalGutter + ((height - 2 * verticalGutter) / labelsList.size.minus(1).toFloat() * index).roundInt(),
                    width = width,
                    readFromLang = readFromLang,
                    activeCol = activeCol,
                    activeBackCol = activeBackCol,
                    activeBackBlendMode = activeBackBlendMode,
                    highlightCol = highlightCol,
                    highlightBackCol = activeBackCol, // we are using custom highlighter
                    highlightBackBlendMode = activeBackBlendMode, // we are using custom highlighter
                    inactiveCol = inactiveCol,
                    preGapX = pregap,
                    postGapX = postgap
            )
        }
    }

    override var posX = 0
    override var posY = 0

    var selectedIndex = defaultSelection
    val selectedButton: UIItemTextButton
        get() = buttons[selectedIndex]
    private var highlightY = buttons[selectedIndex].posY.toDouble()
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


            if (btn.mousePushed && index != selectedIndex) {
                if (kinematic) {
                    highlighterYStart = buttons[selectedIndex].posY.toDouble()
                    selectedIndex = index
                    highlighterMoving = true
                    highlighterYEnd = buttons[selectedIndex].posY.toDouble()
                }
                else {
                    selectedIndex = index
                    highlightY = buttons[selectedIndex].posY.toDouble()
                }
            }
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.color = backgroundCol
        BlendMode.resolve(backgroundBlendMode)
        g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

        g.color = highlightBackCol
        BlendMode.resolve(highlightBackBlendMode)
        g.fillRect(posX.toFloat(), highlightY.toFloat(), width.toFloat(), UIItemTextButton.height.toFloat())

        buttons.forEach { it.render(gc, g) }

        if (iconSpriteSheet != null) {
            val iconY = (buttons[1].height - iconCellHeight) / 2
            iconSpriteSheetIndices!!.forEachIndexed { counter, imageIndex ->
                iconSpriteSheet.getSubImage(imageIndex, 0).draw(
                        32f,
                        buttons[counter].posY + iconY.toFloat()
                )
            }
        }

        g.color = backgroundCol

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