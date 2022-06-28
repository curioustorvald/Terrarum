package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-10-20.
 */
class UIItemInventoryElemSimple(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override var item: GameItem?,
        override var amount: Long,
        override var itemImage: TextureRegion?,
        override var quickslot: Int? = null,
        override var equippedSlot: Int? = null,
        val drawBackOnNull: Boolean = true,
        keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
        touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
        extraInfo: Any? = null
) : UIItemInventoryCellBase(parentUI, initialX, initialY, item, amount, itemImage, quickslot, equippedSlot, keyDownFun, touchDownFun, extraInfo) {
    
    companion object {
        val height = UIItemInventoryElemWide.height
    }

    override val width = UIItemInventoryElemSimple.height
    override val height = UIItemInventoryElemSimple.height

    private val imgOffsetY: Float
        get() = (this.height - itemImage!!.regionHeight).div(2).toFloat() // to snap to the pixel grid
    private val imgOffsetX: Float
        get() = (this.height - itemImage!!.regionWidth).div(2).toFloat() // to snap to the pixel grid

    override fun update(delta: Float) {

    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        // cell background
        if (item != null || drawBackOnNull) {
            batch.color = Toolkit.Theme.COL_CELL_FILL
            Toolkit.fillArea(batch, posX, posY, width, height)
        }
        // cell border
        batch.color = if (equippedSlot != null || forceHighlighted) Toolkit.Theme.COL_HIGHLIGHT
                else if (mouseUp && item != null) Toolkit.Theme.COL_ACTIVE
                else Toolkit.Theme.COL_INVENTORY_CELL_BORDER
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)


        // quickslot and equipped slot indicator is not needed as it's intended for blocks and walls
        // and you can clearly see the quickslot UI anyway

        if (item != null && itemImage != null) {

            // item image
            batch.color = Color.WHITE
            batch.draw(itemImage, posX + imgOffsetX, posY + imgOffsetY)



            // if item has durability, draw that and don't draw count; durability and itemCount cannot coexist
            if (item!!.maxDurability > 0.0) {
                // draw durability metre
                val barFullLen = width
                val barOffset = posX
                val thickness = UIItemInventoryElemWide.durabilityBarThickness
                val percentage = item!!.durability / item!!.maxDurability
                val durabilityCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
                val durabilityBack = durabilityCol mul UIItemInventoryCellCommonRes.meterBackDarkening
                if (item!!.maxDurability > 0.0) {
                    batch.color = durabilityBack
                    Toolkit.drawStraightLine(batch, barOffset, posY + height - thickness, barOffset + barFullLen, thickness, false)
                    batch.color = durabilityCol
                    Toolkit.drawStraightLine(batch, barOffset, posY + height - thickness, barOffset + (barFullLen * percentage).roundToInt(), thickness, false)
                }
            }
            // draw item count when applicable
            else if (item!!.stackable) {
                val amountString = amount.toItemCountText()

                // if mouse is over, text lights up
                // highlight item count (blocks/walls) if the item is equipped
                batch.color = item!!.nameColour mul (
                        if (equippedSlot != null || forceHighlighted) Toolkit.Theme.COL_HIGHLIGHT
                        else if (mouseUp && item != null) Toolkit.Theme.COL_ACTIVE
                        else Color.WHITE
                                                    )

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