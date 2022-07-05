package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.ui.InventoryCellColourTheme
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import kotlin.math.roundToInt

/***
 * Note that the UI will not render if either item or itemImage is null.
 *
 * Created by minjaesong on 2017-03-16.
 */
class UIItemInventoryElemWide(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override var item: GameItem?,
        override var amount: Long,
        override var itemImage: TextureRegion?,
        override var quickslot: Int? = null,
        override var equippedSlot: Int? = null,
        val drawBackOnNull: Boolean = true,
        keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
        touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
        extraInfo: Any? = null,
        highlightEquippedItem: Boolean = true, // for some UIs that only cares about getting equipped slot number but not highlighting
        var colourTheme: InventoryCellColourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme
) : UIItemInventoryCellBase(parentUI, initialX, initialY, item, amount, itemImage, quickslot, equippedSlot, keyDownFun, touchDownFun, extraInfo, highlightEquippedItem) {

    companion object {
        val height = 48
        val UNIQUE_ITEM_HAS_NO_AMOUNT = -1L

        val durabilityBarThickness = 3
    }

    override val height = UIItemInventoryElemWide.height

    private val imgOffsetY: Float
        get() = (this.height - itemImage!!.regionHeight).div(2).toFloat() // to snap to the pixel grid
    private val imgOffsetX: Float
        get() = (this.height - itemImage!!.regionWidth).div(2).toFloat() // NOTE we're using this.height to get horizontal value; this is absofreakinlutely intentional (otherwise images would draw center of this wide cell which is not something we want)

    private val textOffsetX = 50f
    private val textOffsetY = 8f



    private val durabilityBarOffY = 35



    override fun update(delta: Float) {
        if (item != null) {

        }
    }

    private var highlightToMainCol = false
    private var highlightToSubCol = false

    var cellHighlightMainCol = Toolkit.Theme.COL_HIGHLIGHT
    var cellHighlightSubCol = Toolkit.Theme.COL_LIST_DEFAULT
    var cellHighlightMouseUpCol = Toolkit.Theme.COL_ACTIVE
    var cellHighlightNormalCol = Toolkit.Theme.COL_INVENTORY_CELL_BORDER

    var textHighlightMainCol = Toolkit.Theme.COL_HIGHLIGHT
    var textHighlightSubCol = Color.WHITE
    var textHighlightMouseUpCol = Toolkit.Theme.COL_ACTIVE
    var textHighlightNormalCol = Color.WHITE

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        highlightToMainCol = customHighlightRuleMain?.invoke(this) ?: (equippedSlot != null && highlightEquippedItem) || forceHighlighted
        highlightToSubCol = customHighlightRule2?.invoke(this) ?: false

        // cell background
        if (item != null || drawBackOnNull) {
            batch.color = Toolkit.Theme.COL_CELL_FILL
            Toolkit.fillArea(batch, posX, posY, width, height)
        }
        // cell border
        batch.color = if (highlightToMainCol) colourTheme.cellHighlightMainCol
                else if (highlightToSubCol) colourTheme.cellHighlightSubCol
                else if (mouseUp && item != null) colourTheme.cellHighlightMouseUpCol
                else colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)


        if (item != null && itemImage != null) {
            val amountString = amount.toItemCountText()

            blendNormal(batch)
            
            // item image
            batch.color = Color.WHITE
            batch.draw(itemImage, posX + imgOffsetX, posY + imgOffsetY)

            // if mouse is over, text lights up
            // highlight item name and count (blocks/walls) if the item is equipped
            batch.color = item!!.nameColour mul (
                    if (highlightToMainCol) colourTheme.textHighlightMainCol
                    else if (highlightToSubCol) colourTheme.textHighlightSubCol
                    else if (mouseUp && item != null) colourTheme.textHighlightMouseUpCol
                    else colourTheme.textHighlightNormalCol)

            // draw name of the item
            App.fontGame.draw(batch,
                    // print name and amount in parens
                    item!!.name + (if (amount > 0 && item!!.stackable) "\u3000($amountString)" else if (amount != 1L) "\u3000!!$amountString!!" else ""),

                    posX + textOffsetX,
                    posY + textOffsetY
            )


            // durability metre
            val barFullLen = (width - 8) - textOffsetX.toInt()
            val barOffset = posX + textOffsetX.toInt()
            val percentage = if (item!!.maxDurability < 0.00001f) 0f else item!!.durability / item!!.maxDurability
            val durabilityCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
            val durabilityBack = durabilityCol mul UIItemInventoryCellCommonRes.meterBackDarkening
            if (item!!.maxDurability > 0.0) {
                batch.color = durabilityBack
                Toolkit.drawStraightLine(batch, barOffset, posY + durabilityBarOffY, barOffset + barFullLen, durabilityBarThickness, false)
                batch.color = durabilityCol
                Toolkit.drawStraightLine(batch, barOffset, posY + durabilityBarOffY, barOffset + (barFullLen * percentage).roundToInt(), durabilityBarThickness, false)
            }


            // quickslot marker (TEMPORARY UNTIL WE GET BETTER DESIGN)
            batch.color = Color.WHITE

            if (quickslot != null) {
                val label = quickslot!!.plus(0xE010).toChar()
                val labelW = App.fontGame.getWidth("$label")
                App.fontGame.draw(batch, "$label", barOffset + barFullLen - labelW.toFloat(), posY + textOffsetY)
            }

        }

        // see IFs above?
        batch.color = Color.WHITE

    }

    override fun dispose() {
    }
}
