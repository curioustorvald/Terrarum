package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticPushedDown
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-03-13.
 */
class UIItemTextButtonList(
        parentUI: UICanvas,
        val lineHeight: Int,
        labelsList: Array<String>,
        initialX: Int,
        initialY: Int,
        override var width: Int,
        override var height: Int = lineHeight * labelsList.size,
        val readFromLang: Boolean = false,
        val defaultSelection: Int? = null, // negative: INVALID, positive: valid, null: no select

        // icons
        val textAreaWidth: Int,
        val iconSpriteSheet: TextureRegionPack? = null,
        val iconSpriteSheetIndices: IntArray? = null,
        val iconCol: Color = Toolkit.Theme.COL_LIST_DEFAULT,

        // copied directly from UIItemTextButton
        /** Colour when mouse is over */
        val activeCol: Color = Toolkit.Theme.COL_MOUSE_UP,
        /** Colour when mouse is over */
        val activeBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = Toolkit.Theme.COL_SELECTED,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = Toolkit.Theme.COL_LIST_DEFAULT,

        val leftPadding: Int = 0,
        val rightPadding: Int = 0,

        val alignment: UIItemTextButton.Companion.Alignment = UIItemTextButton.Companion.Alignment.CENTRE,
        val itemHitboxSize: Int = lineHeight,

        tagsCollection: Array<Array<String>> = Array(labelsList.size) { arrayOf("") },

        val backgroundCol: Color = Color(0),
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
    val pregap = (width - textAreaWidth - iconsWithGap) / 2 + iconsWithGap + leftPadding
    val postgap = (width - textAreaWidth - iconsWithGap) / 2 + rightPadding



    val buttons = labelsList.mapIndexed { i, s ->
        //val height = this.height - UIItemTextButton.height

        val h = height.toFloat()
        val ss = labelsList.size.toFloat()
        val lh = itemHitboxSize
        val vertOff = lineHeight * i

        val ld0 = { s }
        val ld1 = { Lang[s] }

//        if (!kinematic) {
            UIItemTextButton(
                parentUI, if (readFromLang) ld1 else ld0,
                initialX = posX,
                initialY = posY + vertOff,
                width = width,
                activeCol = activeCol,
                activeBackCol = activeBackCol,
                activeBackBlendMode = activeBackBlendMode,
                highlightCol = highlightCol,
                highlightBackCol = highlightBackCol,
                highlightBackBlendMode = highlightBackBlendMode,
                inactiveCol = inactiveCol,
                paddingLeft = pregap,
                paddingRight = postgap,
                alignment = alignment,
                hitboxSize = itemHitboxSize,
                tags = tagsCollection[i],
            )
//        }
//        else {
//            UIItemTextButton(
//                    parentUI, s,
//                    initialX = posX,
//                    initialY = posY + vertOff,
//                    width = width,
//                    readFromLang = readFromLang,
//                    activeCol = activeCol,
//                    activeBackCol = activeBackCol,
//                    activeBackBlendMode = activeBackBlendMode,
//                    highlightCol = highlightCol,
//                    highlightBackCol = activeBackCol, // we are using custom highlighter
//                    highlightBackBlendMode = activeBackBlendMode, // we are using custom highlighter
//                    inactiveCol = inactiveCol,
//                    paddingLeft = pregap,
//                    paddingRight = postgap,
//                    alignment = alignment,
//                    hitboxSize = itemHitboxSize,
//                    tags = tagsCollection[i]
//            )
//        }
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

    private var clickLatch = MouseLatch()

    override fun show() {
//        printdbg(this, "${this.javaClass.simpleName} show()")
        clickLatch.forceLatch()
    }

    /** (oldIndex: Int?, newIndex: Int) -> Unit */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    override fun update(delta: Float) {
        val posXDelta = posX - oldPosX
        buttons.forEach { it.posX += posXDelta }


        if (highlighterMoving) {
            highlighterMoveTimer += delta

            if (selectedIndex != null) {
                highlightY = Movement.moveQuick(
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

            if (btn.mouseUp) clickLatch.latchNoRelease {
                val oldIndex = selectedIndex

//                if (kinematic) {
//                    selectedIndex = index
//                    highlighterYStart = buttons[selectedIndex!!].posY.toFloat()
//                    highlighterMoving = true
//                    highlighterYEnd = buttons[selectedIndex!!].posY.toFloat()
//                }
//                else {
                    selectedIndex = index
                    highlightY = buttons[selectedIndex!!].posY.toFloat()
//                }

                selectionChangeListener?.invoke(oldIndex, index)
                playHapticPushedDown()
            }
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null
        }

        clickLatch.latch {  } // update to unlatch

        oldPosX = posX
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {

        /*if (kinematic) {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

            batch.color = highlightBackCol
            BlendMode.resolve(highlightBackBlendMode, batch)
            if (highlightY != null) {
                Toolkit.fillArea(batch, posX.toFloat(), highlightY!!.toFloat(), width.toFloat(), itemHitboxSize.toFloat())
            }
        }*/

        batch.color = backgroundCol
        blendNormalStraightAlpha(batch)
        Toolkit.fillArea(batch, posX, posY, width, height)


        buttons.forEach { it.render(frameDelta, batch, camera) }


        if (iconSpriteSheet != null) {
            val iconY = (buttons[1].height - iconCellHeight) / 2
            batch.color = iconCol

            iconSpriteSheetIndices!!.forEachIndexed { counter, imageIndex ->
                batch.draw(iconSpriteSheet.get(imageIndex, 0), 32f, buttons[counter].posY + iconY.toFloat())
            }
        }

//        batch.color = backgroundCol
    }

    fun select(index: Int) {
        selectedIndex = index
        buttons.forEachIndexed { index, btn ->
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null
        }
    }

    fun deselect() {
        selectedIndex = null
        buttons.forEachIndexed { index, btn ->
            btn.highlighted = false
        }
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