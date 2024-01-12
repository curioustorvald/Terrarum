package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.getWidthOfCells
import net.torvald.terrarum.ui.*
import net.torvald.unicode.getKeycapPC
import kotlin.math.max
import kotlin.math.min

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class UIStorageChest : UICanvas(
        toggleKeyLiteral = "control_key_inventory",
        toggleButtonLiteral = "control_gamepad_start",
), HasInventory {

    lateinit var chestInventory: FixtureInventory
    lateinit var chestNameFun: () -> String

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val negotiator = object : InventoryTransactionNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
            player.remove(item, amount)
            fixture.add(item, amount)
        }

        override fun refund(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
            fixture.remove(item, amount)
            player.add(item, amount)
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = chestInventory
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private val catBar: UIItemCatBar
    private val itemListChest: UITemplateHalfInventory
    private val itemListPlayer: UITemplateHalfInventory

    private var encumbrancePerc = 0f
    private var isEncumbered = false

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap * 2) / 2

    init {
        catBar = UIItemCatBar(
            this,
            (width - UIInventoryFull.catBarWidth) / 2,
            42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
            UIInventoryFull.internalWidth,
            UIInventoryFull.catBarWidth,
            false,

            catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category"),
            catArrangement = intArrayOf(9,6,7,1,0,2,1_011,3,4,5,8), // icon order
            catIconsMeaning = listOf( // sortedBy: catArrangement
                arrayOf(UIItemCatBar.CAT_ALL),
                arrayOf(GameItem.Category.BLOCK),
                arrayOf(GameItem.Category.WALL),
                arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
                arrayOf(GameItem.Category.WEAPON),
                arrayOf(GameItem.Category.ARMOUR),
                arrayOf(GameItem.Category.FIXTURE),
                arrayOf(GameItem.Category.GENERIC),
                arrayOf(GameItem.Category.POTION),
                arrayOf(GameItem.Category.MAGIC),
                arrayOf(GameItem.Category.MISC),
            ),
            catIconsLabels = listOf(
                { Lang["MENU_LABEL_ALL"] },
                { Lang["GAME_INVENTORY_BLOCKS"] },
                { Lang["GAME_INVENTORY_WALLS"] },
                { Lang["CONTEXT_ITEM_TOOL_PLURAL"] },
                { Lang["GAME_INVENTORY_WEAPONS"] },
                { Lang["CONTEXT_ITEM_ARMOR"] },
                { Lang["CONTEXT_ITEM_FIXTURES"] },
                { Lang["GAME_INVENTORY_INGREDIENTS"] },
                { Lang["GAME_INVENTORY_POTIONS"] },
                { Lang["CONTEXT_ITEM_MAGIC"] },
                { Lang["GAME_GENRE_MISC"] },
            ),

        )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }


        itemListChest = UITemplateHalfInventory(this, true) { getFixtureInventory() }.also {
            it.itemListKeyDownFun = { _, _, _, _, _ -> Unit }
            it.itemListTouchDownFun = { gameItem, amount, button, _, _ ->
                if (button == App.getConfigInt("config_mouseprimary")) {
                    if (gameItem != null) {
                        negotiator.refund(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
            }
        }
        // make grid mode buttons work together
        itemListChest.itemList.navRemoCon.listButtonListener = { _,_ -> setCompact(false) }
        itemListChest.itemList.navRemoCon.gridButtonListener = { _,_ -> setCompact(true) }


        itemListPlayer = UITemplateHalfInventory(this, false).also {
            it.itemListKeyDownFun = { _, _, _, _, _ -> Unit }
            it.itemListTouchDownFun = { gameItem, amount, button, _, _ ->
                if (button == App.getConfigInt("config_mouseprimary")) {
                    if (gameItem != null) {
                        negotiator.accept(getPlayerInventory(), getFixtureInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
            }
        }
        itemListPlayer.itemList.navRemoCon.listButtonListener = { _,_ -> setCompact(false) }
        itemListPlayer.itemList.navRemoCon.gridButtonListener = { _,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(catBar)
        addUIitem(itemListChest)
        addUIitem(itemListPlayer)
    }

    private var openingClickLatched = false

    override fun show() {
        itemListPlayer.itemList.getInventory = { INGAME.actorNowPlaying!!.inventory }

        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    private fun itemListUpdate() {
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])

        encumbrancePerc = getPlayerInventory().capacity.toFloat() / getPlayerInventory().maxCapacity
        isEncumbered = getPlayerInventory().isEncumbered
    }

    private fun setCompact(yes: Boolean) {
        itemListChest.itemList.isCompactMode = yes
        itemListChest.itemList.navRemoCon.gridModeButtons[0].highlighted = !yes
        itemListChest.itemList.navRemoCon.gridModeButtons[1].highlighted = yes
        itemListChest.itemList.itemPage = 0
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])

        itemListPlayer.itemList.isCompactMode = yes
        itemListPlayer.itemList.navRemoCon.gridModeButtons[0].highlighted = !yes
        itemListPlayer.itemList.navRemoCon.gridModeButtons[1].highlighted = yes
        itemListPlayer.itemList.itemPage = 0
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])

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

    private val thisOffsetX = Toolkit.hdrawWidth - getWidthOfCells(6) - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
    private val thisOffsetY = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    private val cellsWidth = (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 6 - UIItemInventoryItemGrid.listGap

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]} "

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // background fill
        UIInventoryFull.drawBackground(batch, 1f)

        // UI items
        batch.color = Color.WHITE

        catBar.render(frameDelta, batch, camera)
        itemListChest.render(frameDelta, batch, camera)
        itemListPlayer.render(frameDelta, batch, camera)


        blendNormalStraightAlpha(batch)

        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        val chestName = chestNameFun()
        val playerName = INGAME.actorNowPlaying!!.actorValue.getAsString(AVKey.NAME).orEmpty().let { it.ifBlank { Lang["GAME_INVENTORY"] } }
        val encumbBarXPos = itemListPlayer.posX + itemListPlayer.width - UIInventoryCells.weightBarWidth + 36
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
                min(UIInventoryCells.weightBarWidth, max(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
            UIInventoryFull.controlHelpHeight - 6f
        )

        // chest name text
        batch.color = Color.WHITE
        App.fontGame.draw(batch, chestName, thisOffsetX + (cellsWidth - App.fontGame.getWidth(chestName)) / 2, thisOffsetY - 30)
        App.fontGame.draw(batch, playerName, thisOffsetX2 + (cellsWidth - App.fontGame.getWidth(playerName)) / 2, thisOffsetY - 30)

        // control hint
        App.fontGame.draw(batch, controlHelp, thisOffsetX - 34f, encumbBarYPos - 3)

        // encumb text
        batch.color = Color.WHITE
        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }
}