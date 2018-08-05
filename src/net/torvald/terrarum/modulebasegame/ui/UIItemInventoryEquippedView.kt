package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
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

    override val width  = 104
    override val height = 384

    companion object {
        val width  = 104
        val height = 384
    }

    private val listGap = 8

    var itemPage = 0
    var itemPageCount = 1 // TODO total size of current category / itemGrid.size

    lateinit var inventorySortList: Array<GameItem?>
    private var rebuildList = true
    
    val spriteViewBackCol: Color; get() = Color(0x404040_88.toInt())//Color(0xd4d4d4_ff.toInt())

    private val itemGrid = Array<UIItemInventoryCellBase>(
            2 * 5, {
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
    )


    override fun update(delta: Float) {
        itemGrid.forEach { it.update(delta) }
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        // sprite background
        blendNormal()
        batch.color = spriteViewBackCol
        batch.fillRect(
                posX.toFloat(), posY.toFloat(),
                width.toFloat(), width.toFloat()
        )

        // sprite
        val sprite = theActor.sprite
        sprite?.let {
            blendNormal(batch)

            it.render(
                    batch,
                    posX + (width - it.cellWidth).div(2).toFloat(),
                    posY + (width - it.cellHeight).div(2).toFloat()
        ) }


        itemGrid.forEach { it.render(batch, camera) }
    }


    internal fun rebuild() {
        inventorySortList = inventory.itemEquipped.clone()


        
        rebuildList = false

        // sort by equip position

        // fill the grid from fastest index, make no gap in-between of slots
        var listPushCnt = 0
        for (k in 0 until itemGrid.size) {
            val it = inventorySortList[k]

            if (it != null) {
                val itemRecord = inventory.getByDynamicID(it.dynamicID)!!

                itemGrid[listPushCnt].item = it
                itemGrid[listPushCnt].amount = itemRecord.amount
                itemGrid[listPushCnt].itemImage = ItemCodex.getItemImage(it)
                itemGrid[listPushCnt].quickslot = null // don't need to be displayed
                itemGrid[listPushCnt].equippedSlot = null // don't need to be displayed

                listPushCnt++
            }
        }

        // empty out un-filled grids from previous garbage
        for (m in listPushCnt until itemGrid.size) {
            itemGrid[m].item = null
            itemGrid[m].amount = 0
            itemGrid[m].itemImage = null
            itemGrid[m].quickslot = null
            itemGrid[m].equippedSlot = null
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