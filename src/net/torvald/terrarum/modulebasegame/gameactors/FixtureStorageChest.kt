package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.gamepadLabelStart
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory.Companion.CAPACITY_MODE_COUNT
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIInventoryCells.Companion.weightBarWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.catBarWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.controlHelpHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureStorageChest : FixtureBase {

    constructor() : super(
            BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
            mainUI = UIStorageChest(),
            inventory = FixtureInventory(40, CAPACITY_MODE_COUNT),
            nameFun = { Lang["ITEM_STORAGE_CHEST"] }
    ) {

        (mainUI as UIStorageChest).chestInventory = this.inventory!!
        (mainUI as UIStorageChest).chestNameFun = this.nameFun

        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("itemplaceholder_16").texture, 16, 16)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS


        printStackTrace(this)
    }

    override fun reload() {
        super.reload()
        // doing this is required as when things are deserialised, constructor is called, THEN the fields are
        // filled in, thus the initialised mainUI has a stale reference;
        // we fix it by simply giving a new reference to the mainUI
        (mainUI as UIStorageChest).chestInventory = this.inventory!!
        (mainUI as UIStorageChest).chestNameFun = this.nameFun
    }

    companion object {
        const val MASS = 2.0
    }
}


internal class UIStorageChest : UICanvas(
        toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
        toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
), HasInventory {

    lateinit var chestInventory: FixtureInventory
    lateinit var chestNameFun: () -> String

    override var width = App.scr.width
    override var height = App.scr.height
    override var openCloseTime: Second = 0.0f

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

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    init {
        catBar = UIItemInventoryCatBar(
                this,
                (App.scr.width - catBarWidth) / 2,
                42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - internalHeight) / 2,
                internalWidth,
                catBarWidth,
                false
        )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }
        itemListChest = UIItemInventoryItemGrid(
                this,
                catBar,
                { getFixtureInventory() },
                INVENTORY_CELLS_OFFSET_X() - halfSlotOffset,
                INVENTORY_CELLS_OFFSET_Y(),
                6, CELLS_VRT,
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
                INVENTORY_CELLS_OFFSET_X() - halfSlotOffset + (listGap + UIItemInventoryElemWide.height) * 7,
                INVENTORY_CELLS_OFFSET_Y(),
                6, CELLS_VRT,
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
    private val thisOffsetX2 = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 7
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    private val cellsWidth = (listGap + UIItemInventoryElemWide.height) * 6 - listGap

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]} "

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // background fill
        UIInventoryFull.drawBackground(batch)

        // UI items
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemListChest.render(batch, camera)
        itemListPlayer.render(batch, camera)


        blendNormal(batch)

        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        val chestName = chestNameFun()
        val playerName = INGAME.actorNowPlaying!!.actorValue.getAsString(AVKey.NAME).orEmpty().let { it.ifBlank { Lang["GAME_INVENTORY"] } }
        val encumbBarXPos = itemListPlayer.posX + itemListPlayer.width - weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val yEnd = -UIInventoryFull.YPOS_CORRECTION + (App.scr.height + internalHeight).div(2).toFloat() // directly copied from UIInventoryFull.yEnd
        val encumbBarYPos = yEnd - 20 + 3 // dunno why but extra 3 px is needed
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening

        // encumbrance bar background
        batch.color = encumbBack
        Toolkit.fillArea(batch, encumbBarXPos, encumbBarYPos, weightBarWidth, controlHelpHeight - 6f)
        // encumbrance bar
        batch.color = encumbCol
        Toolkit.fillArea(batch, 
                encumbBarXPos, encumbBarYPos,
                if (getPlayerInventory().capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                controlHelpHeight - 6f
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