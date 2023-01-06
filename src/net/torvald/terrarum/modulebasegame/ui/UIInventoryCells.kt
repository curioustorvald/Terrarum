package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_HOR
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.controlHelpHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericKeyDownFun
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.createInvCellGenericTouchDownFun
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

internal class UIInventoryCells(
        val full: UIInventoryFull
) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    companion object {
        val weightBarWidth = UIItemInventoryElemSimple.height * 2f + UIItemInventoryItemGrid.listGap
//        var encumbBarYPos = (App.scr.height + internalHeight).div(2) - 20 + 3f
    }

    internal var encumbrancePerc = 0f
        private set
    internal var isEncumbered = false
        private set


    internal val itemList: UIItemInventoryItemGrid =
            UIItemInventoryItemGrid(
                    full,
                    full.catBar,
                    { full.actor.inventory },
                    INVENTORY_CELLS_OFFSET_X(),
                    INVENTORY_CELLS_OFFSET_Y(),
                    CELLS_HOR, CELLS_VRT,
                    keyDownFun = createInvCellGenericKeyDownFun(),
                    touchDownFun = createInvCellGenericTouchDownFun { rebuildList() }
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
        blendNormalStraightAlpha(batch)
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.listControlHelp, controlHintXPos, full.yEnd - 20)


        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = full.xEnd - weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = full.yEnd-20 + 3f +
                            if (App.fontGame.getWidth(full.listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                App.fontGame.lineHeight
                            else 0f
//        Companion.encumbBarYPos = encumbBarYPos // q&d hack to share some numbers

        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)

        // encumbrance bar background
        blendNormalStraightAlpha(batch)
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = encumbBack
        Toolkit.fillArea(batch, 
                encumbBarXPos, encumbBarYPos,
                weightBarWidth, controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        Toolkit.fillArea(batch, 
                encumbBarXPos, encumbBarYPos,
                if (full.actor.inventory.capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                controlHelpHeight - 6f
        )
        // debug text
        batch.color = Color.LIGHT_GRAY
        if (App.IS_DEVELOPMENT_BUILD) {
            App.fontSmallNumbers.draw(batch,
                    "${full.actor.inventory.capacity}/${full.actor.inventory.maxCapacity}",
                    encumbBarTextXPos,
                    encumbBarYPos + controlHelpHeight - 4f
            )
        }
    }

    override fun show() {
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }


    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun dispose() {
        itemList.dispose()
        equipped.dispose()
    }
}