package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.ui.InventoryCellColourTheme
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.defaultInventoryCellTheme
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.mul
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-10-20.
 */
class UIItemInventoryElemSimple(
    parentUI: UICanvas,
    initialX: Int,
    initialY: Int,
    override var item: GameItem? = null,
    override var amount: Long = UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT,
    override var itemImage: TextureRegion? = null,
    override var quickslot: Int? = null,
    override var equippedSlot: Int? = null, // remnants of wide cell displaying slot number and highlighting; in this style of cell this field only determines highlightedness at render
    val drawBackOnNull: Boolean = true,
    keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
    touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
    extraInfo: Any? = null,
    highlightEquippedItem: Boolean = true, // for some UIs that only cares about getting equipped slot number but not highlighting
    colourTheme: InventoryCellColourTheme = defaultInventoryCellTheme,
    var showItemCount: Boolean = true,
) : UIItemInventoryCellBase(parentUI, initialX, initialY, item, amount, itemImage, quickslot, equippedSlot, keyDownFun, touchDownFun, extraInfo, highlightEquippedItem, colourTheme) {
    
    companion object {
        val height = UIItemInventoryElemWide.height
    }

    override val width = Companion.height
    override val height = Companion.height

    private val itemImageOrDefault: TextureRegion
        get() = itemImage ?: CommonResourcePool.getAsTextureRegion("itemplaceholder_16")

    private val imgOffsetY: Float
        get() = (this.height - itemImageOrDefault.regionHeight).div(2).toFloat() // to snap to the pixel grid
    private val imgOffsetX: Float
        get() = (this.height - itemImageOrDefault.regionWidth).div(2).toFloat() // to snap to the pixel grid

    override fun update(delta: Float) {

    }

    private var highlightToMainCol = false
    private var highlightToSubCol = false

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)

        highlightToMainCol = customHighlightRuleMain?.invoke(this) ?: (equippedSlot != null && highlightEquippedItem) || forceHighlighted
        highlightToSubCol = customHighlightRule2?.invoke(this) ?: false

        // cell background
        if (item != null || drawBackOnNull) {
            batch.color = colourTheme.cellBackgroundCol
            Toolkit.fillArea(batch, posX, posY, width, height)
        }
        // cell border
        batch.color = if (highlightToMainCol) colourTheme.cellHighlightMainCol
                else if (highlightToSubCol) colourTheme.cellHighlightSubCol
                else if (mouseUp && item != null) colourTheme.cellHighlightMouseUpCol
                else colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)


        // quickslot and equipped slot indicator is not needed as it's intended for blocks and walls
        // and you can clearly see the quickslot UI anyway

        if (item != null) {

            // item image
            batch.color = Color.WHITE
            batch.draw(itemImageOrDefault, posX + imgOffsetX, posY + imgOffsetY)



            // if item has durability, draw that and don't draw count; durability and itemCount cannot coexist
            if (item!!.maxDurability > 0.0) {
                // draw durability metre
                val barFullLen = width
                val barOffset = posX
                val thickness = UIItemInventoryElemWide.durabilityBarThickness
                val percentage = item!!.durability / item!!.maxDurability
                val durabilityCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
                val durabilityBack = durabilityCol mul UIItemInventoryCellCommonRes.meterBackDarkening

                batch.color = durabilityBack
                Toolkit.drawStraightLine(batch, barOffset, posY + height - thickness, barOffset + barFullLen, thickness, false)
                batch.color = durabilityCol
                Toolkit.drawStraightLine(batch, barOffset, posY + height - thickness, barOffset + (barFullLen * percentage).roundToInt(), thickness, false)
            }
            // draw item count when applicable
            else if (item!!.stackable && showItemCount) {
                val amountString = amount.toItemCountText()

                // if mouse is over, text lights up
                // highlight item count (blocks/walls) if the item is equipped
                batch.color = item!!.nameColour mul (
                        if (highlightToMainCol) colourTheme.textHighlightMainCol
                        else if (highlightToSubCol) colourTheme.textHighlightSubCol
                        else if (mouseUp && item != null) colourTheme.textHighlightMouseUpCol
                        else colourTheme.textHighlightNormalCol)

                App.fontSmallNumbers.draw(batch,
                        amountString,
                        posX + (width - App.fontSmallNumbers.getWidth(amountString)).toFloat(),
                        posY + (height - App.fontSmallNumbers.H).toFloat()
                )
            }

        }

        // see IFs above?
        batch.color = Color.WHITE

    }

    override fun dispose() {
    }
}