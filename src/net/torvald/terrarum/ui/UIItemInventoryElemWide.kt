package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.ui.InventoryCellColourTheme
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.mul
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
    override var item: GameItem? = null,
    override var amount: Long = UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT,
    override var itemImage: TextureRegion? = null,
    override var quickslot: Int? = null,
    override var equippedSlot: Int? = null,
    val drawBackOnNull: Boolean = true,
    keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
    touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
    extraInfo: Any? = null,
    highlightEquippedItem: Boolean = true, // for some UIs that only cares about getting equipped slot number but not highlighting
    colourTheme: InventoryCellColourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme,
    var showItemCount: Boolean = true,
) : UIItemInventoryCellBase(parentUI, initialX, initialY, item, amount, itemImage, quickslot, equippedSlot, keyDownFun, touchDownFun, extraInfo, highlightEquippedItem, colourTheme) {

    companion object {
        val height = 48
        val UNIQUE_ITEM_HAS_NO_AMOUNT = -1L

        val durabilityBarThickness = 3
    }

    override val height = Companion.height

    private val itemImageOrDefault: TextureRegion
        get() = itemImage ?: CommonResourcePool.getAsTextureRegion("itemplaceholder_16")

    private val imgOffsetY: Float
        get() = (this.height - itemImageOrDefault.regionHeight).div(2).toFloat() // to snap to the pixel grid
    private val imgOffsetX: Float
        get() = (this.height - itemImageOrDefault.regionWidth).div(2).toFloat() // NOTE we're using this.height to get horizontal value; this is absofreakinlutely intentional (otherwise images would draw center of this wide cell which is not something we want)

    private val textOffsetX = 50f
    private val textOffsetY = 8f



    private val durabilityBarOffY = 35



    override fun update(delta: Float) {
        if (item != null) {

        }
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


        if (item != null) {
            val amountString = amount.toItemCountText()

            blendNormalStraightAlpha(batch)
            
            // item image
            batch.color = Color.WHITE
            batch.draw(itemImageOrDefault, posX + imgOffsetX, posY + imgOffsetY)

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
                if (showItemCount)
                    item!!.name + (if (amount > 0 && item!!.stackable) "\u3000($amountString)" else if (amount != 1L) "\u3000!!$amountString!!" else "")
                else
                    item!!.name,

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
