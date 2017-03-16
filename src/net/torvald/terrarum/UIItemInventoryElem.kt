package net.torvald.terrarum

import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * @param amount: set to -1 (UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT) for unique item (does not show item count)
 *
 * Note that the UI will not render if either item or itemImage is null.
 *
 * Created by SKYHi14 on 2017-03-16.
 */
class UIItemInventoryElem(
        parentUI: UICanvas,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        var item: InventoryItem?,
        var amount: Int,
        var itemImage: Image?,
        val backCol: Color = Color(0,0,0,0),
        val backColBlendMode: String = BlendMode.NORMAL
) : UIItem(parentUI) {

    companion object {
        val height = 48
        val UNIQUE_ITEM_HAS_NO_AMOUNT = -1
    }

    override val height = UIItemInventoryElem.height

    private val imgOffset: Float
        get() = (this.height - itemImage!!.height).div(2).toFloat() // to snap to the pixel grid
    private val textOffsetX = 52f

    override fun update(gc: GameContainer, delta: Int) {
        if (item != null) {

        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        if (item != null && itemImage != null) {
            g.font = Terrarum.fontGame


            if (mouseUp) {
                BlendMode.resolve(backColBlendMode)
                g.color = backCol
                g.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
            }


            blendNormal()

            g.drawImage(itemImage!!, posX + imgOffset, posY + imgOffset)

            // if mouse is over, text lights up
            g.color = item!!.nameColour * if (mouseUp) Color(0xffffff) else UIItemTextButton.defaultInactiveCol
            g.drawString(item!!.name, posX + textOffsetX, posY + 0f)


            if (item!!.maxDurability > 0.0) {
                // TODO durability gauge
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