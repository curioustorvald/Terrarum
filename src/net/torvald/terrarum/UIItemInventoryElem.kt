package net.torvald.terrarum

import net.torvald.colourutil.CIELabUtil.darkerLab
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIInventory
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/***
 * Note that the UI will not render if either item or itemImage is null.
 *
 * Created by SKYHi14 on 2017-03-16.
 */
class UIItemInventoryElem(
        parentUI: UIInventory,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        var item: InventoryItem?,
        var amount: Int,
        var itemImage: Image?,
        val mouseOverTextCol: Color = Color(0xfff066),
        val mouseoverBackCol: Color = Color(0,0,0,0),
        val mouseoverBackBlendMode: String = BlendMode.NORMAL,
        val inactiveTextCol: Color = UIItemTextButton.defaultInactiveCol,
        val backCol: Color = Color(0,0,0,0),
        val backBlendMode: String = BlendMode.NORMAL,
        var quickslot: Int? = null,
        var equippedSlot: Int? = null,
        val drawBackOnNull: Boolean = true
) : UIItem(parentUI) {

    companion object {
        val height = 48
        val UNIQUE_ITEM_HAS_NO_AMOUNT = -1
    }

    private val inventoryUI = parentUI

    override val height = UIItemInventoryElem.height

    private val imgOffset: Float
        get() = (this.height - itemImage!!.height).div(2).toFloat() // to snap to the pixel grid
    private val textOffsetX = 50f
    private val textOffsetY = 8f


    private val durabilityCol = Color(0x22ff11)
    private val durabilityBack: Color; get() = durabilityCol.darkerLab(0.4f)
    private val durabilityBarOffY = 35f



    override fun update(gc: GameContainer, delta: Int) {
        if (item != null) {

        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.font = Terrarum.fontGame

        // mouseover background
        if (item != null || drawBackOnNull) {
            // do not highlight even if drawBackOnNull is true
            if (mouseUp && item != null) {
                BlendMode.resolve(mouseoverBackBlendMode)
                g.color = mouseoverBackCol
            }
            // if drawBackOnNull, just draw background
            else {
                BlendMode.resolve(backBlendMode)
                g.color = backCol
            }
            g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }


        if (item != null && itemImage != null) {
            blendNormal()

            // item image
            g.drawImage(itemImage!!, posX + imgOffset, posY + imgOffset)

            // if mouse is over, text lights up
            // this one-liner sets color
            g.color = item!!.nameColour mul if (mouseUp) mouseOverTextCol else inactiveTextCol
            g.drawString(
                    item!!.name + (if (amount > 0 && !item!!.isUnique) "${0x3000.toChar()}($amount)" else "") +
                    (if (equippedSlot != null) "  ${0xE081.toChar()}\$$equippedSlot" else ""),
                    posX + textOffsetX,
                    posY + textOffsetY
            )


            // durability metre
            val barFullLen = (width - 8f) - textOffsetX
            val barOffset = posX + textOffsetX
            if (item!!.maxDurability > 0.0) {
                g.color = durabilityBack
                g.lineWidth = 3f
                g.drawLine(barOffset, posY + durabilityBarOffY, barOffset + barFullLen, posY + durabilityBarOffY)
                g.color = durabilityCol
                g.drawLine(barOffset, posY + durabilityBarOffY, barOffset + barFullLen * (item!!.durability / item!!.maxDurability), posY + durabilityBarOffY)
            }


            // quickslot marker (TEMPORARY UNTIL WE GET BETTER DESIGN)
            if (quickslot != null) {
                val label = quickslot!!.plus(0xE010).toChar()
                val labelW = g.font.getWidth("$label")
                g.color = Color.white
                g.drawString("$label", barOffset + barFullLen - labelW, posY + textOffsetY)
            }

        }
    }

    override fun keyPressed(key: Int, c: Char) {
    }

    override fun keyReleased(key: Int, c: Char) {
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
        if (item != null && Terrarum.ingame != null) {
            // equip da shit
            val itemEquipSlot = item!!.equipPosition
            val player = Terrarum.ingame!!.player

            if (item != player?.inventory?.itemEquipped?.get(itemEquipSlot)) { // if this item is unequipped, equip it
                player?.equipItem(item!!)
            }
            else { // if not, unequip it
                player?.unequipItem(item!!)
            }
        }

        inventoryUI.rebuildList()
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
    }

    override fun mouseWheelMoved(change: Int) {
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
    }
}