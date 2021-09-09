package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVEN_DEBUG_MODE
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.Toolkit.DEFAULT_BOX_BORDER_COL
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton

/***
 * Note that the UI will not render if either item or itemImage is null.
 *
 * Created by minjaesong on 2017-03-16.
 */
class UIItemInventoryElem(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override var item: GameItem?,
        override var amount: Int,
        override var itemImage: TextureRegion?,
        val mouseOverTextCol: Color = Color(0xfff066_ff.toInt()),
        val mouseoverBackCol: Color = Color(0),
        val mouseoverBackBlendMode: String = BlendMode.NORMAL,
        val inactiveTextCol: Color = UIItemTextButton.defaultInactiveCol,
        val backCol: Color = Color(0),
        val backBlendMode: String = BlendMode.NORMAL,
        override var quickslot: Int? = null,
        override var equippedSlot: Int? = null,
        val drawBackOnNull: Boolean = true,
        keyDownFun: (GameItem?, Int, Int) -> Unit,
        touchDownFun: (GameItem?, Int, Int) -> Unit
) : UIItemInventoryCellBase(parentUI, initialX, initialY, item, amount, itemImage, quickslot, equippedSlot, keyDownFun, touchDownFun) {

    companion object {
        val height = 48
        val UNIQUE_ITEM_HAS_NO_AMOUNT = -1

        internal val durabilityBarThickness = 3f
    }

    override val height = UIItemInventoryElem.height

    private val imgOffset: Float
        get() = (this.height - itemImage!!.regionHeight).div(2).toFloat() // to snap to the pixel grid
    private val textOffsetX = 50f
    private val textOffsetY = 8f



    private val durabilityBarOffY = 35f



    override fun update(delta: Float) {
        if (item != null) {

        }
    }

    private val fwsp = 0x3000.toChar()

    override fun render(batch: SpriteBatch, camera: Camera) {

        // mouseover background
        if (item != null || drawBackOnNull) {
            // do not highlight even if drawBackOnNull is true
            if (mouseUp && item != null) {
                BlendMode.resolve(mouseoverBackBlendMode, batch)
                batch.color = mouseoverBackCol
            }
            // if drawBackOnNull, just draw background
            else {
                BlendMode.resolve(backBlendMode, batch)
                batch.color = backCol
            }
            Toolkit.fillArea(batch, posX, posY, width, height)
        }
        batch.color = DEFAULT_BOX_BORDER_COL
        //blendNormal(batch)
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)


        if (item != null && itemImage != null) {
            val amountString = amount.toItemCountText()

            blendNormal(batch)
            
            // item image
            batch.color = Color.WHITE
            batch.draw(itemImage, posX + imgOffset, posY + imgOffset)

            // if mouse is over, text lights up
            // this one-liner sets color
            batch.color = item!!.nameColour mul if (mouseUp) mouseOverTextCol else inactiveTextCol
            // draw name of the item
            if (INVEN_DEBUG_MODE) {
                App.fontGame.draw(batch,
                        // print static id, dynamic id, and count
                        "${item!!.originalID}/${item!!.dynamicID}" + (if (amount > 0 && item!!.stackable) "$fwsp($amountString)" else if (amount != 1) "$fwsp!!$amountString!!" else ""),
                        posX + textOffsetX,
                        posY + textOffsetY
                )
            }
            else {
                App.fontGame.draw(batch,
                        // print name and amount in parens
                        item!!.name + (if (amount > 0 && item!!.stackable) "$fwsp($amountString)" else if (amount != 1) "$fwsp!!$amountString!!" else "") +
                        // TEMPORARY print eqipped slot info as well
                        (if (equippedSlot != null) "  ${0xE081.toChar()}\$$equippedSlot" else ""),

                        posX + textOffsetX,
                        posY + textOffsetY
                )
            }


            // durability metre
            val barFullLen = (width - 8f) - textOffsetX
            val barOffset = posX + textOffsetX
            val percentage = if (item!!.maxDurability < 0.00001f) 0f else item!!.durability / item!!.maxDurability
            val durabilityCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
            val durabilityBack = durabilityCol mul UIItemInventoryCellCommonRes.meterBackDarkening
            if (item!!.maxDurability > 0.0) {
                batch.color = durabilityBack
                batch.drawStraightLine(barOffset, posY + durabilityBarOffY, barOffset + barFullLen, durabilityBarThickness, false)
                batch.color = durabilityCol
                batch.drawStraightLine(barOffset, posY + durabilityBarOffY, barOffset + barFullLen * percentage, durabilityBarThickness, false)
            }


            // quickslot marker (TEMPORARY UNTIL WE GET BETTER DESIGN)
            batch.color = Color.WHITE

            if (quickslot != null) {
                val label = quickslot!!.plus(0xE010).toChar()
                val labelW = App.fontGame.getWidth("$label")
                App.fontGame.draw(batch, "$label", barOffset + barFullLen - labelW, posY + textOffsetY)
            }

        }

        // see IFs above?
        batch.color = Color.WHITE

    }

    override fun dispose() {
    }
}
