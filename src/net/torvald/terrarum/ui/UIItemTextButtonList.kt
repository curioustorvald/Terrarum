package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-03-13.
 */
class UIItemTextButtonList(
        parentUI: UICanvas,
        labelsList: Array<String>,
        initialX: Int,
        initialY: Int,
        override var width: Int,
        override var height: Int = DEFAULT_LINE_HEIGHT * labelsList.size,
        val readFromLang: Boolean = false,
        val defaultSelection: Int? = null, // negative: INVALID, positive: valid, null: no select

        // icons
        val textAreaWidth: Int,
        val iconSpriteSheet: TextureRegionPack? = null,
        val iconSpriteSheetIndices: IntArray? = null,
        val iconCol: Color = UIItemTextButton.defaultInactiveCol,

        // copied directly from UIItemTextButton
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


        val kinematic: Boolean = false,

        val alignment: UIItemTextButton.Companion.Alignment = UIItemTextButton.Companion.Alignment.CENTRE,
        val itemHitboxSize: Int = DEFAULT_LINE_HEIGHT
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        val DEFAULT_BACKGROUNDCOL = Color(0x242424_80)
        val DEFAULT_BACKGROUND_HIGHLIGHTCOL = Color(0x121212BF)
        val DEFAULT_BACKGROUND_ACTIVECOL = Color(0x1b1b1b9F)
        val DEFAULT_LINE_HEIGHT = 36
    }

    val iconToTextGap = 20
    val iconCellWidth = (iconSpriteSheet?.tileW ?: -iconToTextGap) / (iconSpriteSheet?.horizontalCount ?: 1)
    val iconCellHeight = (iconSpriteSheet?.tileH ?: 0) / (iconSpriteSheet?.verticalCount ?: 1)

    // zero if iconSpriteSheet is null
    val iconsWithGap: Int = iconToTextGap + iconCellWidth
    val pregap = (width - textAreaWidth - iconsWithGap) / 2 + iconsWithGap
    val postgap = (width - textAreaWidth - iconsWithGap) / 2



    val buttons = labelsList.mapIndexed { i, s ->
        //val height = this.height - UIItemTextButton.height

        val h = height.toFloat()
        val ss = labelsList.size.toFloat()
        val lh = itemHitboxSize
        val vertOff = (h/ss * i + (h/ss - lh) / 2f).roundToInt()

        if (!kinematic) {
            UIItemTextButton(
                    parentUI, s,
                    initialX = posX,
                    initialY = posY + vertOff,
                    width = width,
                    readFromLang = readFromLang,
                    activeCol = activeCol,
                    activeBackCol = activeBackCol,
                    activeBackBlendMode = activeBackBlendMode,
                    highlightCol = highlightCol,
                    highlightBackCol = highlightBackCol,
                    highlightBackBlendMode = highlightBackBlendMode,
                    inactiveCol = inactiveCol,
                    backgroundCol = backgroundCol,
                    backgroundBlendMode = backgroundBlendMode,
                    preGapX = pregap,
                    postGapX = postgap,
                    alignment = alignment,
                    hitboxSize = itemHitboxSize
            )
        }
        else {
            UIItemTextButton(
                    parentUI, s,
                    initialX = posX,
                    initialY = posY + vertOff,
                    width = width,
                    readFromLang = readFromLang,
                    activeCol = activeCol,
                    activeBackCol = activeBackCol,
                    activeBackBlendMode = activeBackBlendMode,
                    highlightCol = highlightCol,
                    highlightBackCol = activeBackCol, // we are using custom highlighter
                    highlightBackBlendMode = activeBackBlendMode, // we are using custom highlighter
                    backgroundCol = Color(0),
                    inactiveCol = inactiveCol,
                    preGapX = pregap,
                    postGapX = postgap,
                    alignment = alignment,
                    hitboxSize = itemHitboxSize
            )
        }
    }


    /*initialX = 0
        set(value) {
            buttons.forEach {
                val oldPosX = field
                val newPosX = value
                it.posX = (newPosX - oldPosX)
            }
            field = value
        }
    initialY = 0
        set(value) {
            buttons.forEach {
                val oldPosY = field
                val newPosY = value
                it.posY = (newPosY - oldPosY)
            }
            field = value
        }*/


    var selectedIndex: Int? = defaultSelection
    val selectedButton: UIItemTextButton?
        get() = if (selectedIndex != null) buttons[selectedIndex!!] else null
    private var highlightY: Float? = if (selectedIndex != null) buttons[selectedIndex!!].posY.toFloat() else null
    private val highlighterMoveDuration: Second = 0.1f
    private var highlighterMoveTimer: Second = 0f
    private var highlighterMoving = false
    private var highlighterYStart = highlightY
    private var highlighterYEnd = highlightY

    /** (oldIndex: Int?, newIndex: Int) -> Unit */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    override fun update(delta: Float) {
        val posXDelta = posX - oldPosX
        buttons.forEach { it.posX += posXDelta }


        if (highlighterMoving) {
            highlighterMoveTimer += delta

            if (selectedIndex != null) {
                highlightY = UIUtils.moveQuick(
                        highlighterYStart!!,
                        highlighterYEnd!!,
                        highlighterMoveTimer.toFloat(),
                        highlighterMoveDuration.toFloat()
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
                val oldIndex = selectedIndex

                if (kinematic) {
                    selectedIndex = index
                    highlighterYStart = buttons[selectedIndex!!].posY.toFloat()
                    highlighterMoving = true
                    highlighterYEnd = buttons[selectedIndex!!].posY.toFloat()
                }
                else {
                    selectedIndex = index
                    highlightY = buttons[selectedIndex!!].posY.toFloat()
                }

                selectionChangeListener?.invoke(oldIndex, index)
            }
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

        }

        oldPosX = posX
    }

    override fun render(batch: SpriteBatch, camera: Camera) {

        if (kinematic) {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

            batch.color = highlightBackCol
            BlendMode.resolve(highlightBackBlendMode, batch)
            if (highlightY != null) {
                batch.fillRect(posX.toFloat(), highlightY!!.toFloat(), width.toFloat(), itemHitboxSize.toFloat())
            }
        }

        buttons.forEach { it.render(batch, camera) }


        if (iconSpriteSheet != null) {
            val iconY = (buttons[1].height - iconCellHeight) / 2
            batch.color = iconCol

            iconSpriteSheetIndices!!.forEachIndexed { counter, imageIndex ->
                batch.draw(iconSpriteSheet.get(imageIndex, 0), 32f, buttons[counter].posY + iconY.toFloat())
            }
        }

        batch.color = backgroundCol
    }

    fun select(index: Int) {
        selectedIndex = index
    }

    fun deselect() {
        selectedIndex = null
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

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return super.scrolled(amountX, amountY) || buttons.map { it.scrolled(amountX, amountY).toInt() }.sum() != 0
    }
}