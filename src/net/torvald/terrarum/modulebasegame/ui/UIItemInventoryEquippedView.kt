package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.itemListHeight
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericKeyDownFun
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericTouchDownFun
import net.torvald.terrarum.spriteassembler.ADProperties.Companion.EXTRA_HEADROOM_X
import net.torvald.terrarum.spriteassembler.ADProperties.Companion.EXTRA_HEADROOM_Y
import net.torvald.terrarum.ui.*

/**
 * Created by minjaesong on 2017-10-28.
 */
class UIItemInventoryEquippedView(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        inventoryListRebuildFun: () -> Unit
) : UIItem(parentUI, initialX, initialY) {


    override val width  = WIDTH
    override val height = itemListHeight

    companion object {
        val WIDTH  = 2 * UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap
        //val HEIGHT = UIItemInventoryDynamicList.HEIGHT
        val SPRITE_DRAW_COL = Color(0xf2f2f2ff.toInt())
    }

    private val listGap = 8

    var itemPage = 0
    var itemPageCount = 1 // TODO total size of current category / itemGrid.size

    lateinit var inventorySortList: Array<GameItem?>
    private var rebuildList = true

    private val equipPosIcon = CommonResourcePool.getAsTextureRegionPack("inventory_category")
    private val cellToIcon = intArrayOf(0,1,2,3,4,5,6,7,6,7,6,7)

    private val itemGrid = Array<UIItemInventoryCellBase>(2 * 6) {
        UIItemInventoryElemSimple(
            parentUI = parentUI,
            initialX = this.posX + (UIItemInventoryElemSimple.height + listGap) * ((it + 4) % 2),
            initialY = this.posY + (UIItemInventoryElemSimple.height + listGap) * ((it + 4) / 2),
            item = null,
            amount = UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT,
            itemImage = null,
            drawBackOnNull = true,
            keyDownFun = { _, _, _, _, _ -> },
            touchDownFun = createInvCellGenericTouchDownFun(inventoryListRebuildFun), // to "unselect" the equipped item and main item grid would "untick" accordingly
            wheelFun = { _, _, _, _, _, _ -> },
            emptyCellIcon = equipPosIcon.get(cellToIcon[it], 1),
        )
    }


    override fun update(delta: Float) {
        itemGrid.forEach { it.update(delta) }
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)

        val posXDelta = posX - oldPosX
        itemGrid.forEach { it.posX += posXDelta }

        // sprite cell background
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, posX, posY, width, width)
        // sprite cell border
        batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
        Toolkit.drawBoxBorder(batch, posX, posY, width, width)


        // sprite
        INGAME.actorNowPlaying?.let { actor ->
            actor.sprite?.let {
                blendNormalStraightAlpha(batch)

                batch.color = SPRITE_DRAW_COL

                if (it is SheetSpriteAnimation) {
                    batch.draw(
                            it.textureRegion.get(0, 0),
                            posX + (width - it.cellWidth + EXTRA_HEADROOM_X).div(2).toFloat(),
                            posY + (width - it.cellHeight - EXTRA_HEADROOM_Y).div(2).toFloat()
                    )
                }
                else if (it is AssembledSpriteAnimation) {
                    it.renderThisAnimation(batch,
                            posX + ((width - actor.baseHitboxW) / 2).toFloat(),
                            posY + ((width - actor.baseHitboxH) / 2).toFloat(),
                            1f, "ANIM_IDLE_1"
                    )
                }

            }
        }

        // slot image on each cells
        itemGrid.forEachIndexed { index, cell ->
            cell.render(frameDelta, batch, camera)
        }


        oldPosX = posX
    }

    internal fun rebuild() {
        rebuildList = false

        INGAME.actorNowPlaying?.inventory?.let {
            // sort by equip position

            // fill the grid from fastest index, make no gap in-between of slots
            for (k in itemGrid.indices) {
                val item = it.itemEquipped[k]

                if (item == null) {

                    itemGrid[k].item = null
                    itemGrid[k].amount = 0
                    itemGrid[k].itemImage = null
                    itemGrid[k].quickslot = null
                    itemGrid[k].equippedSlot = null
                }
                else {
                    val itemRecord = it.searchByID(item)!!

                    itemGrid[k].item = ItemCodex[item]
                    itemGrid[k].amount = itemRecord.qty
                    itemGrid[k].itemImage = ItemCodex.getItemImage(item)
                    itemGrid[k].quickslot = null // don't need to be displayed
                    itemGrid[k].equippedSlot = null // don't need to be displayed
                }
            }
        }
    }
    
    
    override fun dispose() {
        itemGrid.forEach { it.dispose() }
        // equipPosIcon.dispose() // disposed of by the AppLoader
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