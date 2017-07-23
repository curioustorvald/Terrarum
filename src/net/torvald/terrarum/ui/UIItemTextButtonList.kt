package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameactors.ai.toInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.inUse
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-03-13.
 */
class UIItemTextButtonList(
        parentUI: UICanvas,
        labelsList: Array<String>,
        override var width: Int,
        override var height: Int,
        val verticalGutter: Int = 0,
        val readFromLang: Boolean = false,
        val defaultSelection: Int? = null, // negative: INVALID, positive: valid, null: no select

        // icons
        val textAreaWidth: Int,
        val iconSpriteSheet: TextureRegionPack? = null,
        val iconSpriteSheetIndices: IntArray? = null,
        val iconCol: Color = UIItemTextButton.defaultInactiveCol,

        // copied directly from UIItemTextButton
        val activeCol: Color = Color(0xfff066_ff.toInt()),
        val activeBackCol: Color = Color(0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Color(0x00f8ff_ff),
        val highlightBackCol: Color = Color(0xb0b0b0_ff.toInt()),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = Color(0xc0c0c0_ff.toInt()),
        val backgroundCol: Color = Color(0x242424_80),
        val backgroundBlendMode: String = BlendMode.NORMAL,
        val kinematic: Boolean = false
) : UIItem(parentUI) {

    val iconToTextGap = 20
    val iconCellWidth = (iconSpriteSheet?.tileW ?: -iconToTextGap) / (iconSpriteSheet?.horizontalCount ?: 1)
    val iconCellHeight = (iconSpriteSheet?.tileH ?: 0) / (iconSpriteSheet?.verticalCount ?: 1)

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

    var selectedIndex: Int? = defaultSelection
    val selectedButton: UIItemTextButton?
        get() = if (selectedIndex != null) buttons[selectedIndex!!] else null
    private var highlightY: Double? = if (selectedIndex != null) buttons[selectedIndex!!].posY.toDouble() else null
    private val highlighterMoveDuration: Second = 0.1f
    private var highlighterMoveTimer: Second = 0f
    private var highlighterMoving = false
    private var highlighterYStart = highlightY
    private var highlighterYEnd = highlightY


    override fun update(delta: Float) {
        if (highlighterMoving) {
            highlighterMoveTimer += delta

            if (selectedIndex != null) {
                highlightY = UIUtils.moveQuick(
                        highlighterYStart!!,
                        highlighterYEnd!!,
                        highlighterMoveTimer.toDouble(),
                        highlighterMoveDuration.toDouble()
                )
            }

            if (highlighterMoveTimer > highlighterMoveDuration) {
                highlighterMoveTimer = 0f
                highlighterYStart = highlighterYEnd
                highlightY = highlighterYEnd
                highlighterMoving = false
            }
        }

        buttons.forEachIndexed { index, btn ->
            btn.update(delta)


            if (btn.mousePushed && index != selectedIndex) {
                if (kinematic) {
                    highlighterYStart = buttons[selectedIndex!!].posY.toDouble()
                    selectedIndex = index
                    highlighterMoving = true
                    highlighterYEnd = buttons[selectedIndex!!].posY.toDouble()
                }
                else {
                    selectedIndex = index
                    highlightY = buttons[selectedIndex!!].posY.toDouble()
                }
            }
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

        }
    }

    override fun render(batch: SpriteBatch) {
        batch.color = backgroundCol
        BlendMode.resolve(backgroundBlendMode)
        batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

        batch.color = highlightBackCol
        BlendMode.resolve(highlightBackBlendMode)
        if (highlightY != null) {
            batch.fillRect(posX.toFloat(), highlightY!!.toFloat(), width.toFloat(), UIItemTextButton.height.toFloat())
        }

        buttons.forEach { it.render(batch) }


        if (iconSpriteSheet != null) {
            val iconY = (buttons[1].height - iconCellHeight) / 2
            batch.color = iconCol

            iconSpriteSheetIndices!!.forEachIndexed { counter, imageIndex ->
                batch.draw(iconSpriteSheet.get(imageIndex, 0), 32f, buttons[counter].posY + iconY.toFloat())
            }
        }

        batch.color = backgroundCol
    }

    override fun dispose() {
        iconSpriteSheet?.dispose()
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode) || buttons.map { it.keyDown(keycode).toInt() }.sum() != 0
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode) || buttons.map { it.keyUp(keycode).toInt() }.sum() != 0
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY) || buttons.map { it.mouseMoved(screenX, screenY).toInt() }.sum() != 0
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer) || buttons.map { it.touchDragged(screenX, screenY, pointer).toInt() }.sum() != 0
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button) || buttons.map { it.touchDown(screenX, screenY, pointer, button).toInt() }.sum() != 0
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button) || buttons.map { it.touchUp(screenX, screenY, pointer, button).toInt() }.sum() != 0
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount) || buttons.map { it.scrolled(amount).toInt() }.sum() != 0
    }
}