package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_HOR
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVEN_DEBUG_MODE
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericKeyDownFun
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericTouchDownFun
import net.torvald.terrarum.ui.UICanvas

internal class UIInventoryCells(
        val full: UIInventoryFull
) : UICanvas() {

    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH
    override var openCloseTime: Second = 0.0f


    private val weightBarWidth = UIItemInventoryElemSimple.height * 2f + UIItemInventoryItemGrid.listGap

    internal var encumbrancePerc = 0f
        private set
    internal var isEncumbered = false
        private set


    internal val itemList: UIItemInventoryItemGrid =
            UIItemInventoryItemGrid(
                    full,
                    full.catBar,
                    full.actor.inventory,
                    INVENTORY_CELLS_OFFSET_X,
                    INVENTORY_CELLS_OFFSET_Y,
                    CELLS_HOR, CELLS_VRT,
                    keyDownFun = createInvCellGenericKeyDownFun(),
                    touchDownFun = createInvCellGenericTouchDownFun { rebuildList() }
            )


    private val equipped: UIItemInventoryEquippedView =
            UIItemInventoryEquippedView(
                    full,
                    full.actor.inventory,
                    full.actor as ActorWithBody,
                    internalWidth - UIItemInventoryEquippedView.WIDTH + (AppLoader.screenW - internalWidth) / 2,
                    INVENTORY_CELLS_OFFSET_Y
            )

    init {
        uiItems.add(itemList)
        uiItems.add(equipped)
    }

    fun rebuildList() {
        AppLoader.printdbg(this, "rebuilding list")

        itemList.rebuild(full.catBar.catIconsMeaning[full.catBar.selectedIcon])
        equipped.rebuild()

        encumbrancePerc = full.actor.inventory.capacity.toFloat() / full.actor.inventory.maxCapacity
        isEncumbered = full.actor.inventory.isEncumbered
    }

    fun resetStatusAsCatChanges(oldcat: Int?, newcat: Int) {
        itemList.itemPage = 0 // set scroll to zero
        itemList.rebuild(full.catBar.catIconsMeaning[full.catBar.catArrangement[newcat]]) // have to manually rebuild, too!
    }

    override fun updateUI(delta: Float) {
        itemList.update(delta)
        equipped.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        //itemList.posX = itemList.initialX + inventoryScrOffX.roundToInt()
        itemList.render(batch, camera)
        //equipped.posX = equipped.initialX + inventoryScrOffX.roundToInt()
        equipped.render(batch, camera)


        // control hints
        val controlHintXPos = full.offsetX
        blendNormal(batch)
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, full.listControlHelp, controlHintXPos, full.yEnd - 20)


        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = full.xEnd - weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - AppLoader.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = full.yEnd-20 + 3f +
                            if (AppLoader.fontGame.getWidth(full.listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                AppLoader.fontGame.lineHeight
                            else 0f

        AppLoader.fontGame.draw(batch,
                encumbranceText,
                encumbBarTextXPos,
                encumbBarYPos - 3f
        )

        // encumbrance bar background
        blendNormal(batch)
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = encumbBack
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                weightBarWidth, full.controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                if (full.actor.inventory.capacityMode == ActorInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                full.controlHelpHeight - 6f
        )
        // debug text
        batch.color = Color.LIGHT_GRAY
        if (INVEN_DEBUG_MODE) {
            AppLoader.fontSmallNumbers.draw(batch,
                    "${full.actor.inventory.capacity}/${full.actor.inventory.maxCapacity}",
                    encumbBarTextXPos,
                    encumbBarYPos + full.controlHelpHeight - 4f
            )
        }
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        itemList.dispose()
        equipped.dispose()
    }
}