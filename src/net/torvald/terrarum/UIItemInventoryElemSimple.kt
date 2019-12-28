package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.Toolkit.DEFAULT_BOX_BORDER_COL
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * Created by minjaesong on 2017-10-20.
 */
class UIItemInventoryElemSimple(
        parentUI: UIInventoryFull,
        override var posX: Int,
        override var posY: Int,
        override var item: GameItem?,
        override var amount: Int,
        override var itemImage: TextureRegion?,
        val mouseOverTextCol: Color = Color(0xfff066_ff.toInt()),
        val mouseoverBackCol: Color = Color(0),
        val mouseoverBackBlendMode: String = BlendMode.NORMAL,
        val inactiveTextCol: Color = UIItemTextButton.defaultInactiveCol,
        val backCol: Color = Color(0),
        val backBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = UIItemTextButton.defaultHighlightCol,
        override var quickslot: Int? = null,
        override var equippedSlot: Int? = null,
        val drawBackOnNull: Boolean = true
) : UIItemInventoryCellBase(parentUI, posX, posY, item, amount, itemImage, quickslot, equippedSlot) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    companion object {
        val height = UIItemInventoryElem.height
    }

    private val inventoryUI = parentUI

    override val width = UIItemInventoryElemSimple.height
    override val height = UIItemInventoryElemSimple.height

    private val imgOffset: Float
        get() = (this.height - itemImage!!.regionHeight).div(2).toFloat() // to snap to the pixel grid

    override fun update(delta: Float) {
        if (item != null) {

        }
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        // mouseover background
        if (item != null || drawBackOnNull) {
            // do not highlight even if drawBackOnNull is true
            if (mouseUp && item != null || equippedSlot != null) { // "equippedSlot != null": also highlight back if equipped
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


        // quickslot and equipped slot indicator is not needed as it's intended for blocks and walls
        // and you can clearly see the quickslot UI anyway

        if (item != null && itemImage != null) {
            blendNormal(batch)

            // item image
            batch.color = Color.WHITE
            batch.draw(itemImage, posX + imgOffset, posY + imgOffset)

            // if mouse is over, text lights up
            // this one-liner sets color
            batch.color = item!!.nameColour mul if (mouseUp) mouseOverTextCol else inactiveTextCol


            // if item has durability, draw that and don't draw count; durability and itemCount cannot coexist
            if (item!!.maxDurability > 0.0) {
                // draw durability metre
                val barFullLen = width
                val barOffset = posX.toFloat()
                val thickness = UIItemInventoryElem.durabilityBarThickness
                val percentage = item!!.durability / item!!.maxDurability
                val durabilityCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
                val durabilityBack = durabilityCol mul UIItemInventoryCellCommonRes.meterBackDarkening
                if (item!!.maxDurability > 0.0) {
                    batch.color = durabilityBack
                    batch.drawStraightLine(barOffset, posY + height - thickness, barOffset + barFullLen, thickness, false)
                    batch.color = durabilityCol
                    batch.drawStraightLine(barOffset, posY + height - thickness, barOffset + barFullLen * percentage, thickness, false)
                }
            }
            // draw item count when applicable
            else if (item!!.stackable) {
                val amountString = amount.toItemCountText()

                // highlight item count (blocks/walls) if the item is equipped
                if (equippedSlot != null) {
                    batch.color = highlightCol
                }


                AppLoader.fontSmallNumbers.draw(batch,
                        amountString,
                        posX + (width - AppLoader.fontSmallNumbers.getWidth(amountString)).toFloat(),
                        posY + (height - AppLoader.fontSmallNumbers.H).toFloat()
                )
            }

        }

        // see IFs above?
        batch.color = Color.WHITE

    }

    override fun keyDown(keycode: Int): Boolean {
        if (item != null && Terrarum.ingame != null && keycode in Input.Keys.NUM_0..Input.Keys.NUM_9) {

            val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
            if (player == null) return false

            val inventory = player.inventory
            val slot = if (keycode == Input.Keys.NUM_0) 9 else keycode - Input.Keys.NUM_1
            val currentSlotItem = inventory.getQuickslot(slot)


            inventory.setQuickBar(
                    slot,
                    if (currentSlotItem?.item != item?.dynamicID)
                        item?.dynamicID // register
                    else
                        null // drop registration
            )

            // search for duplicates in the quickbar, except mine
            // if there is, unregister the other
            (0..9).minus(slot).forEach {
                if (inventory.getQuickslot(it)?.item == item?.dynamicID) {
                    inventory.setQuickBar(it, null)
                }
            }
        }

        return super.keyDown(keycode)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        //println("touchdown elemgrid")

        if (item != null && Terrarum.ingame != null) {

            // equip da shit
            val itemEquipSlot = item!!.equipPosition
            val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
            if (player == null) return false

            if (item != ItemCodex[player.inventory.itemEquipped.get(itemEquipSlot)]) { // if this item is unequipped, equip it
                player.equipItem(item!!)
            }
            else { // if not, unequip it
                player.unequipItem(item!!)
            }
        }

        inventoryUI.rebuildList()

        return super.touchDown(screenX, screenY, pointer, button)
    }


    override fun dispose() {
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }

}