package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class UIStorageChest : UICanvas(
        toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
        toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
), HasInventory {

    lateinit var chestInventory: FixtureInventory
    lateinit var chestNameFun: () -> String

    override var width = App.scr.width
    override var height = App.scr.height

    private val negotiator = object : InventoryTransactionNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
            player.remove(item, amount)
            fixture.add(item, amount)
        }

        override fun reject(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
            fixture.remove(item, amount)
            player.add(item, amount)
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = chestInventory
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private val catBar: UIItemInventoryCatBar
    private val itemListChest: UIItemInventoryItemGrid
    private val itemListPlayer: UIItemInventoryItemGrid

    private var encumbrancePerc = 0f
    private var isEncumbered = false

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2

    init {
        catBar = UIItemInventoryCatBar(
            this,
            (App.scr.width - UIInventoryFull.catBarWidth) / 2,
            42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
            UIInventoryFull.internalWidth,
            UIInventoryFull.catBarWidth,
            false
        )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }
        itemListChest = UIItemInventoryItemGrid(
            this,
            catBar,
            { getFixtureInventory() },
            UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset,
            UIInventoryFull.INVENTORY_CELLS_OFFSET_Y(),
            6, UIInventoryFull.CELLS_VRT,
            drawScrollOnRightside = false,
            drawWallet = false,
            keyDownFun = { _, _, _, _, _ -> Unit },
            touchDownFun = { gameItem, amount, _, _, _ ->
                if (gameItem != null) {
                    negotiator.reject(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                }
                itemListUpdate()
            }
        )
        // make grid mode buttons work together
        itemListChest.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListChest.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        itemListPlayer = UIItemInventoryItemGrid(
            this,
            catBar,
            { INGAME.actorNowPlaying!!.inventory }, // literally a player's inventory
            UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7,
            UIInventoryFull.INVENTORY_CELLS_OFFSET_Y(),
            6, UIInventoryFull.CELLS_VRT,
            drawScrollOnRightside = true,
            drawWallet = false,
            keyDownFun = { _, _, _, _, _ -> Unit },
            touchDownFun = { gameItem, amount, _, _, _ ->
                if (gameItem != null) {
                    negotiator.accept(getPlayerInventory(), getFixtureInventory(), gameItem, amount)
                }
                itemListUpdate()
            }
        )
        itemListPlayer.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListPlayer.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(catBar)
        addUIitem(itemListChest)
        addUIitem(itemListPlayer)
    }

    private var openingClickLatched = false

    override fun show() {
        itemListPlayer.getInventory = { INGAME.actorNowPlaying!!.inventory }

        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    private fun itemListUpdate() {
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        encumbrancePerc = getPlayerInventory().capacity.toFloat() / getPlayerInventory().maxCapacity
        isEncumbered = getPlayerInventory().isEncumbered
    }

    private fun setCompact(yes: Boolean) {
        itemListChest.isCompactMode = yes
        itemListChest.gridModeButtons[0].highlighted = !yes
        itemListChest.gridModeButtons[1].highlighted = yes
        itemListChest.itemPage = 0
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

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
        catBar.update(delta)
        itemListChest.update(delta)
        itemListPlayer.update(delta)

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
    private val thisOffsetY = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    private val cellsWidth = (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 6 - UIItemInventoryItemGrid.listGap

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]} "

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // background fill
        UIInventoryFull.drawBackground(batch, handler.opacity)

        // UI items
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemListChest.render(batch, camera)
        itemListPlayer.render(batch, camera)


        blendNormalStraightAlpha(batch)

        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        val chestName = chestNameFun()
        val playerName = INGAME.actorNowPlaying!!.actorValue.getAsString(AVKey.NAME).orEmpty().let { it.ifBlank { Lang["GAME_INVENTORY"] } }
        val encumbBarXPos = itemListPlayer.posX + itemListPlayer.width - UIInventoryCells.weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val yEnd = -UIInventoryFull.YPOS_CORRECTION + (App.scr.height + UIInventoryFull.internalHeight).div(2).toFloat() // directly copied from UIInventoryFull.yEnd
        val encumbBarYPos = yEnd - 20 + 3 // dunno why but extra 3 px is needed
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening

        // encumbrance bar background
        batch.color = encumbBack
        Toolkit.fillArea(
            batch,
            encumbBarXPos,
            encumbBarYPos,
            UIInventoryCells.weightBarWidth,
            UIInventoryFull.controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        Toolkit.fillArea(
            batch,
            encumbBarXPos, encumbBarYPos,
            if (getPlayerInventory().capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                1f
            else // make sure 1px is always be seen
                minOf(UIInventoryCells.weightBarWidth, maxOf(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
            UIInventoryFull.controlHelpHeight - 6f
        )

        // chest name text
        batch.color = Color.WHITE
        App.fontGame.draw(batch, chestName, thisOffsetX + (cellsWidth - App.fontGame.getWidth(chestName)) / 2, thisOffsetY - 30)
        App.fontGame.draw(batch, playerName, thisOffsetX2 + (cellsWidth - App.fontGame.getWidth(playerName)) / 2, thisOffsetY - 30)

        // control hint
        App.fontGame.draw(batch, controlHelp, thisOffsetX + 2f, encumbBarYPos - 3)

        // encumb text
        batch.color = Color.WHITE
        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
    }

    override fun doOpening(delta: Float) {
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }
}