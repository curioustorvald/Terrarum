package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.toInt
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemImageButton

/**
 * Created by minjaesong on 2023-06-17.
 */
class UIItemListNavBarVertical(
    parentUI: UICanvas, initialX: Int, initialY: Int,
    override val height: Int,
    val hasGridModeButtons: Boolean, initialModeSelection: Int = 0,
    private val colourTheme: InventoryCellColourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme,
    var extraDrawOpOnBottom: (UIItemListNavBarVertical, SpriteBatch) -> Unit = { _,_ -> }
) : UIItem(parentUI, initialX, initialY) {

    override var suppressHaptic = true
    override val width = UIItemListNavBarVertical.WIDTH

    override val mouseUp: Boolean
        get() = itemRelativeMouseX - 8 in 0 until width &&
                itemRelativeMouseY + 8 in 0 until height

    companion object {
        const val WIDTH = 28
        const val LIST_TO_CONTROL_GAP = 12
    }

    fun getIconPosY(index: Int) =
        if (index >= 0)
            posY + 26 * index
        else
            (posY + height) + (26 * index)

    private val catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category")
    private val iconPosX = posX + LIST_TO_CONTROL_GAP

    val gridModeButtons = Array<UIItemImageButton>(2) { index ->
        UIItemImageButton(
            parentUI,
            catIcons.get(index + 14, 0),
            initialX = iconPosX,
            initialY = getIconPosY(index),
            highlightable = true
        )
    }

    val scrollUpButton = UIItemImageButton(
        parentUI,
        catIcons.get(18, 0),
        initialX = iconPosX,
        initialY = getIconPosY(2 - (!hasGridModeButtons).toInt(1)),
        highlightable = false
    )

    val scrollDownButton = UIItemImageButton(
        parentUI,
        catIcons.get(19, 0),
        initialX = iconPosX,
        initialY = getIconPosY(3 - (!hasGridModeButtons).toInt(1)),
        highlightable = false
    )

    private val upDownButtonGapToDots = 7 // apparent gap may vary depend on the texture itself

    init {
        gridModeButtons[initialModeSelection].highlighted = true

        gridModeButtons[0].touchDownListener = { _, _, _, _ ->
            gridModeButtons[0].highlighted = true
            gridModeButtons[1].highlighted = false
            itemPage = 0
            listButtonListener(this, gridModeButtons[0])
        }
        gridModeButtons[1].touchDownListener = { _, _, _, _ ->
            gridModeButtons[0].highlighted = false
            gridModeButtons[1].highlighted = true
            itemPage = 0
            gridButtonListener(this, gridModeButtons[1])
        }

        scrollUpButton.clickOnceListener = { _, _ ->
            scrollUpButton.highlighted = false
            scrollUpListener(this, scrollUpButton)
        }
        scrollDownButton.clickOnceListener = { _, _ ->
            scrollDownButton.highlighted = false
            scrollDownListener(this, scrollDownButton)
        }
    }


    var listButtonListener: (UIItemListNavBarVertical, UIItemImageButton) -> Unit = { _,_ -> }
    var gridButtonListener: (UIItemListNavBarVertical, UIItemImageButton) -> Unit = { _,_ -> }
    var scrollUpListener: (UIItemListNavBarVertical, UIItemImageButton) -> Unit = { _,_ -> }
    var scrollDownListener: (UIItemListNavBarVertical, UIItemImageButton) -> Unit = { _,_ -> }
    var itemPageCount = 0
    var itemPage = 0


    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        val posXDelta = posX - oldPosX

        gridModeButtons.forEach { it.posX += posXDelta }
        scrollUpButton.posX += posXDelta
        scrollDownButton.posX += posXDelta

        fun getScrollDotYHeight(i: Int) = scrollUpButton.posY + 14 + upDownButtonGapToDots + 10 * i

        scrollDownButton.posY = getScrollDotYHeight(itemPageCount) + upDownButtonGapToDots



        // draw the tray
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, iconPosX - 4, getIconPosY(0) - 8, width, height)
        // cell border
//        batch.color = if (mouseUp) colourTheme.cellHighlightMouseUpCol else colourTheme.cellHighlightNormalCol // just to test the mouseUp
        batch.color = colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, iconPosX - 4, getIconPosY(0) - 8, width, height)

        if (hasGridModeButtons) gridModeButtons.forEach { it.render(frameDelta, batch, camera) }
        scrollUpButton.render(frameDelta, batch, camera)
        scrollDownButton.render(frameDelta, batch, camera)

        // draw scroll dots
        for (i in 0 until itemPageCount) {
            val colour = if (i == itemPage) Color.WHITE else Color(0xffffff7f.toInt())

            batch.color = colour
            batch.draw(
                catIcons.get(if (i == itemPage) 20 else 21, 0),
                iconPosX.toFloat(),
                getScrollDotYHeight(i) - 2f
            )
        }



        extraDrawOpOnBottom(this, batch)

        super.render(frameDelta, batch, camera)

        oldPosX = posX
    }

    override fun update(delta: Float) {

        if (hasGridModeButtons) gridModeButtons.forEach { it.update(delta) }
        scrollUpButton.update(delta)
        scrollDownButton.update(delta)

        super.update(delta)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (hasGridModeButtons) gridModeButtons.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }
        if (scrollUpButton.mouseUp) scrollUpButton.touchDown(screenX, screenY, pointer, button)
        if (scrollDownButton.mouseUp) scrollDownButton.touchDown(screenX, screenY, pointer, button)
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (mouseUp && amountY > 0f) {
            scrollDownListener.invoke(this, scrollDownButton)
            return true
        }
        else if (mouseUp && amountY < 0f) {
            scrollUpListener.invoke(this, scrollUpButton)
            return true
        }

        return false
    }

    override fun dispose() {
    }


}