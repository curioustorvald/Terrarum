package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_HOR
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.controlHelpHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericKeyDownFun
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericTouchDownFun
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemInventoryElemSimple
import org.dyn4j.geometry.Vector2
import kotlin.math.max
import kotlin.math.min

internal class UIInventoryCells(
        val full: UIInventoryFull
) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    companion object {
        val weightBarWidth = UIItemInventoryElemSimple.height * 2f + UIItemInventoryItemGrid.listGap
//        var encumbBarYPos = (App.scr.height + internalHeight).div(2) - 20 + 3f

        fun drawEncumbranceBar(batch: SpriteBatch, encumbBarXPos: Float, encumbBarYPos: Float, encumbrancePerc: Float, actorInventory: ActorInventory) {
            //draw player encumb
            // encumbrance meter
            val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
            // encumbrance bar will go one row down if control help message is too long
            val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
            App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
            // encumbrance bar fracme
            batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
            Toolkit.drawBoxBorder(batch, encumbBarXPos, encumbBarYPos, UIInventoryCells.weightBarWidth, UIInventoryFull.controlHelpHeight - 6f)
            // encumbrance bar background
            blendNormalStraightAlpha(batch)
            val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
            val encumbBack = Toolkit.Theme.COL_CELL_FILL
            batch.color = encumbBack
            Toolkit.fillArea(
                batch,
                encumbBarXPos, encumbBarYPos,
                UIInventoryCells.weightBarWidth, UIInventoryFull.controlHelpHeight - 6f
            )
            // encumbrance bar
            batch.color = encumbCol
            Toolkit.fillArea(
                batch,
                encumbBarXPos, encumbBarYPos,
                if (actorInventory.capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    min(UIInventoryCells.weightBarWidth, max(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
                UIInventoryFull.controlHelpHeight - 6f
            )

            // tooltip
            if (Terrarum.mouseScreenX.toFloat() in encumbBarXPos..encumbBarXPos+UIInventoryCells.weightBarWidth && Terrarum.mouseScreenY.toFloat() in encumbBarYPos..encumbBarYPos+UIInventoryFull.controlHelpHeight - 6f) {
                val capaStr = if (actorInventory.capacity >= 1125899906842624.0) /* 2^50 */
                    "${actorInventory.capacity}"
                else
                    "${(actorInventory.capacity * 100L).toLong() / 100.0}"

                INGAME.setTooltipMessage("$capaStr/${actorInventory.maxCapacityByActor}")
                tooltipShowing[10001] = true
            }
            else {
                tooltipShowing[10001] = false
            }
        }
    }

    internal var encumbrancePerc = 0f
        private set
    internal var isEncumbered = false
        private set


    internal val itemList: UIItemInventoryItemGrid =
            UIItemInventoryItemGrid(
                full,
                { full.actor.inventory },
                INVENTORY_CELLS_OFFSET_X(),
                INVENTORY_CELLS_OFFSET_Y(),
                CELLS_HOR, CELLS_VRT,
                keyDownFun = createInvCellGenericKeyDownFun { rebuildList() },
                touchDownFun = createInvCellGenericTouchDownFun { rebuildList() },
                wheelFun = { _, _, _, _, _, _ -> },
            )


    private val equipped: UIItemInventoryEquippedView =
            UIItemInventoryEquippedView(
                    full,
                    internalWidth - UIItemInventoryEquippedView.WIDTH + (width - internalWidth) / 2,
                    INVENTORY_CELLS_OFFSET_Y(),
                    { rebuildList() }
            )

    init {
        uiItems.add(itemList)
        uiItems.add(equipped)
    }

    fun rebuildList() {
//        App.printdbg(this, "rebuilding list")

        itemList.rebuild(full.catBar.catIconsMeaning[full.catBar.selectedIndex])
        equipped.rebuild()

        encumbrancePerc = full.actor.inventory.encumberment.toFloat()
        isEncumbered = full.actor.inventory.isEncumbered
    }

    fun resetStatusAsCatChanges(oldcat: Int?, newcat: Int) {
        itemList.itemPage = 0 // set scroll to zero
        itemList.rebuild(full.catBar.catIconsMeaning[newcat]) // have to manually rebuild, too!
    }

    override fun updateImpl(delta: Float) {
        itemList.update(delta)
        equipped.update(delta)

        // make tossing work on the inventory ui
        if (Gdx.input.isKeyJustPressed(ControlPresets.getKey("control_key_discard"))) {
            itemList.find { it.mouseUp }?.let { cell -> cell.item?.let { item ->
                val player = full.actor
                // remove an item from the inventory
                player.inventory.remove(item, 1)
                // create and spawn the droppeditem
                DroppedItem(item.dynamicID,
                    player.hitbox.centeredX,
                    player.hitbox.centeredY,
                    Vector2(-4.0 * player.scale.sqrt() * player.sprite!!.flipHorizontal.toInt(1).minus(1), -0.1)
                ).let { drop ->
                    INGAME.queueActorAddition(drop)
                }
                // apply item effect
                item.effectOnThrow(player)
                // update inventory
                rebuildList()
            } }
        }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        //itemList.posX = itemList.initialX + inventoryScrOffX.roundToInt()
        itemList.render(frameDelta, batch, camera)
        //equipped.posX = equipped.initialX + inventoryScrOffX.roundToInt()
        equipped.render(frameDelta, batch, camera)


        // control hints
        val controlHintXPos = full.offsetX - 34
        blendNormalStraightAlpha(batch)
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.listControlHelp, controlHintXPos, UIInventoryFull.yEnd - 20)


        // encumbrance meter
        val encumbBarXPos = UIInventoryFull.xEnd - weightBarWidth
        val encumbBarYPos = UIInventoryFull.yEnd-20 + 3f

        UIInventoryCells.drawEncumbranceBar(batch, encumbBarXPos, encumbBarYPos, encumbrancePerc, full.actor.inventory)
    }

    override fun show() {
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }


    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun dispose() {
        itemList.dispose()
        equipped.dispose()
    }
}