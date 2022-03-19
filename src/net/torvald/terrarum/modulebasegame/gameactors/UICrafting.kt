package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemSpinner
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * This UI has inventory, but it's just there to display all craftable items and should not be serialised.
 *
 * Created by minjaesong on 2022-03-10.
 */
class UICrafting(val full: UIInventoryFull) : UICanvas(
        toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
        toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
), HasInventory {

    private val catBar: UIItemInventoryCatBar
        get() = full.catBar

    override var width = App.scr.width
    override var height = App.scr.height
    override var openCloseTime: Second = 0.0f

    private val itemListPlayer: UIItemInventoryItemGrid
    private val itemListCraftable: UIItemInventoryItemGrid
    private val buttonCraft: UIItemTextButton
    private val spinnerCraftCount: UIItemSpinner

    private val fakeInventory = FixtureInventory()

    private val negotiator = object : InventoryNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }

        override fun reject(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = fakeInventory
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 7
    private val thisXend = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 13 - listGap
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

    init {
        val craftButtonsY = thisOffsetY + 23 + (UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 1)
        val buttonWidth = (UIItemInventoryElemWide.height + listGap) * 3 - listGap - 2

        // crafting list to the left
        itemListCraftable = UIItemInventoryItemGrid(
                this,
                catBar,
                { getFixtureInventory() },
                thisOffsetX,
                thisOffsetY,
                6, UIInventoryFull.CELLS_VRT - 1, // decrease the internal height so that craft/cancel button would fit in
                drawScrollOnRightside = false,
                drawWallet = false,
                keyDownFun = { _, _, _ -> Unit },
                touchDownFun = { gameItem, amount, _ ->
                    if (gameItem != null) {
                        negotiator.reject(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
        )

        buttonCraft = UIItemTextButton(this, "GAME_ACTION_CRAFT", thisOffsetX + 3 + buttonWidth + listGap, craftButtonsY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
        spinnerCraftCount = UIItemSpinner(this, thisOffsetX + 1, craftButtonsY, 1, 1, 100, 1, buttonWidth)

        buttonCraft.touchDownListener = { _,_,_,_ ->
            printdbg(this, "Craft!")
        }

        // make grid mode buttons work together
        itemListCraftable.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListCraftable.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        // player inventory to the right
        itemListPlayer = UIItemInventoryItemGrid(
                this,
                catBar,
                { INGAME.actorNowPlaying!!.inventory }, // literally a player's inventory
                thisOffsetX2,
                thisOffsetY,
                6, UIInventoryFull.CELLS_VRT,
                drawScrollOnRightside = true,
                drawWallet = false,
                keyDownFun = { _, _, _ -> Unit },
                touchDownFun = { gameItem, amount, _ ->
                    if (gameItem != null) {
                        negotiator.accept(getPlayerInventory(), getFixtureInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
        )
        itemListPlayer.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListPlayer.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(itemListCraftable)
        addUIitem(itemListPlayer)
        addUIitem(spinnerCraftCount)
        addUIitem(buttonCraft)
    }

    // reset whatever player has selected to null and bring UI to its initial state
    fun resetUI() {

    }

    private var openingClickLatched = false

    override fun show() {
        itemListPlayer.getInventory = { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        spinnerCraftCount.value = 1
        spinnerCraftCount.fboUpdateLatch = true

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        itemListCraftable.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }

    private fun setCompact(yes: Boolean) {
        itemListCraftable.isCompactMode = yes
        itemListCraftable.gridModeButtons[0].highlighted = !yes
        itemListCraftable.gridModeButtons[1].highlighted = yes
        itemListCraftable.itemPage = 0
        itemListCraftable.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListPlayer.isCompactMode = yes
        itemListPlayer.gridModeButtons[0].highlighted = !yes
        itemListPlayer.gridModeButtons[1].highlighted = yes
        itemListPlayer.itemPage = 0
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListUpdate()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!openingClickLatched) {
            return super.touchDown(screenX, screenY, pointer, button)
        }
        return false
    }

    override fun updateUI(delta: Float) {
        // NO super.update due to an infinite recursion
        this.uiItems.forEach { it.update(delta) }

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // NO super.render due to an infinite recursion
        this.uiItems.forEach { it.render(batch, camera) }

        batch.color = Color.WHITE

        // text label for two inventory grids
        App.fontGame.draw(batch, Lang["GAME_CRAFTING"], thisOffsetX + 2, thisOffsetY - 30)
        App.fontGame.draw(batch, Lang["GAME_INVENTORY"], thisOffsetX2 + 2, thisOffsetY - 30)


        // control hints
        val controlHintXPos = thisOffsetX.toFloat()
        blendNormal(batch)
        App.fontGame.draw(batch, full.listControlHelp, controlHintXPos, full.yEnd - 20)

        

        //draw player encumb
        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = thisXend - UIInventoryCells.weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = full.yEnd-20 + 3f +
                            if (App.fontGame.getWidth(full.listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                App.fontGame.lineHeight
                            else 0f
        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
        // encumbrance bar background
        blendNormal(batch)
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = encumbBack
        Toolkit.fillArea(batch,
                encumbBarXPos, encumbBarYPos,
                UIInventoryCells.weightBarWidth, UIInventoryFull.controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        Toolkit.fillArea(batch,
                encumbBarXPos, encumbBarYPos,
                if (full.actor.inventory.capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(UIInventoryCells.weightBarWidth, maxOf(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
                UIInventoryFull.controlHelpHeight - 6f
        )
        // debug text
        batch.color = Color.LIGHT_GRAY
        if (App.IS_DEVELOPMENT_BUILD) {
            App.fontSmallNumbers.draw(batch,
                    "${full.actor.inventory.capacity}/${full.actor.inventory.maxCapacity}",
                    encumbBarTextXPos,
                    encumbBarYPos + UIInventoryFull.controlHelpHeight - 4f
            )
        }


        blendNormal(batch)
    }

    override fun doOpening(delta: Float) {
//        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
//        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {

        spinnerCraftCount.value = 1 // hide() is required as show() is not called unless the parent's panel number has changed (?)
        spinnerCraftCount.fboUpdateLatch = true

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }
}