package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.ui.UIItem

/**
 * Created by minjaesong on 2017-10-28.
 */
class UIItemInventoryEquippedView(
        parentUI: UIInventoryFull,
        val inventory: ActorInventory,
        val theActor: ActorWBMovable,
        override var posX: Int,
        override var posY: Int
) : UIItem(parentUI) {

    override val width  = WIDTH
    override val height = HEIGHT

    companion object {
        val WIDTH  = 2 * UIItemInventoryElemSimple.height + UIItemInventoryDynamicList.listGap
        val HEIGHT = UIItemInventoryDynamicList.HEIGHT
    }

    private val listGap = 8

    var itemPage = 0
    var itemPageCount = 1 // TODO total size of current category / itemGrid.size

    lateinit var inventorySortList: Array<GameItem?>
    private var rebuildList = true
    
    val spriteViewBackCol: Color; get() = Color(0x404040_88.toInt())//Color(0xd4d4d4_ff.toInt())

    private val itemGrid = Array<UIItemInventoryCellBase>(2 * 6) {
        UIItemInventoryElemSimple(
                parentUI = parentUI,
                posX = this.posX + (UIItemInventoryElemSimple.height + listGap) * ((it + 4) % 2),
                posY = this.posY + (UIItemInventoryElemSimple.height + listGap) * ((it + 4) / 2),
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(0x282828_ff),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = Color(0x404040_88),
                backBlendMode = BlendMode.NORMAL,
                drawBackOnNull = true
        )
    }


    override fun update(delta: Float) {
        itemGrid.forEach { it.update(delta) }
    }

    private val spriteDrawCol = Color(0xddddddff.toInt())

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    override fun render(batch: SpriteBatch, camera: Camera) {
        val posXDelta = posX - oldPosX
        itemGrid.forEach { it.posX += posXDelta }



        // sprite background
        blendNormal(batch)
        batch.color = spriteViewBackCol
        batch.fillRect(
                posX.toFloat(), posY.toFloat(),
                width.toFloat(), width.toFloat()
        )

        // sprite
        val sprite = theActor.sprite
        sprite?.let {
            blendNormal(batch)

            batch.color = spriteDrawCol
            batch.draw(
                    it.textureRegion.get(0, 0),
                    posX + (width - it.cellWidth).div(2).toFloat(),
                    posY + (width - it.cellHeight).div(2).toFloat()
            )

        }

        // TODO inscribe slot image on each cells HERE

        itemGrid.forEach { it.render(batch, camera) }


        oldPosX = posX
    }

    internal fun rebuild() {
        rebuildList = false

        // sort by equip position

        // fill the grid from fastest index, make no gap in-between of slots
        for (k in 0 until itemGrid.size) {
            val item = inventory.itemEquipped[k]

            if (item == null) {

                itemGrid[k].item = null
                itemGrid[k].amount = 0
                itemGrid[k].itemImage = null
                itemGrid[k].quickslot = null
                itemGrid[k].equippedSlot = null
            }
            else {
                val itemRecord = inventory.getByDynamicID(item.dynamicID)!!

                itemGrid[k].item = item
                itemGrid[k].amount = itemRecord.amount
                itemGrid[k].itemImage = ItemCodex.getItemImage(item)
                itemGrid[k].quickslot = null // don't need to be displayed
                itemGrid[k].equippedSlot = null // don't need to be displayed
            }
        }
    }
    
    
    override fun dispose() {
        itemGrid.forEach { it.dispose() }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        itemGrid.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        itemGrid.forEach { if (it.mouseUp) it.touchUp(screenX, screenY, pointer, button) }

        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        super.keyDown(keycode)

        itemGrid.forEach { if (it.mouseUp) it.keyDown(keycode) }
        rebuild()

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)

        itemGrid.forEach { if (it.mouseUp) it.keyUp(keycode) }
        rebuild()

        return true
    }
}