package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2024-01-29.
 */
class UISmelterBasic() : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
), HasInventory {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val playerThings = UITemplateHalfInventory(this, false).also {
        it.itemListTouchDownFun = { gameItem, _, _, _, _ ->


        }
    }

    private val negotiator = object : InventoryTransactionNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }

        override fun refund(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = TODO()
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    init {
        CommonResourcePool.addToLoadingList("basegame_gui_smelter_icons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/smelter_icons.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val smelterCellIcons = CommonResourcePool.getAsTextureRegionPack("basegame_gui_smelter_icons")

    private var smelterBackdrop =
        FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_tall.tga")

    private var fuelCaloriesNow = 0f
    private var fuelCaloriesMax: Float? = null

    private var temperature = 0f // 0f..1f
    private var progress = 0f


    private val leftPanelWidth = playerThings.width
    private val leftPanelHeight = playerThings.height
    private val leftPanelX = playerThings.posX - leftPanelWidth - UIItemInventoryItemGrid.listGap - UIItemInventoryElemWide.height
    private val leftPanelY = playerThings.posY

    private val backdropColour = Color(0x7f7f7fff)
    private val backdropZoom = 6
    private val backdropX = (leftPanelX + (leftPanelWidth - smelterBackdrop.regionWidth * backdropZoom) / 2).toFloat()
    private val backdropY = (leftPanelY + (leftPanelHeight - smelterBackdrop.regionHeight * backdropZoom) / 2).toFloat()

    private val oreX = backdropX + 12 * backdropZoom + 3
    private val oreY = backdropY + 23 * backdropZoom + 3

    private val fireboxX = backdropX + 12 * backdropZoom + 3
    private val fireboxY = backdropY + 39 * backdropZoom + 3

    private val productX = backdropX + 37 * backdropZoom + 3
    private val productY = backdropY + 39 * backdropZoom + 3

    private val oreItemSlot = UIItemInventoryElemSimple(
        this, oreX.toInt(), oreY.toInt(),
        emptyCellIcon = smelterCellIcons.get(1, 1),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { item, amount, _, _, _ -> }
    )
    private val fireboxItemSlot = UIItemInventoryElemSimple(
        this, fireboxX.toInt(), fireboxY.toInt(),
        emptyCellIcon = smelterCellIcons.get(0, 0),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { item, amount, _, _, _ -> }
    )
    private val productItemslot = UIItemInventoryElemSimple(
        this, productX.toInt(), productY.toInt(),
        emptyCellIcon = smelterCellIcons.get(1, 0),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { item, amount, _, _, _ -> }
    )

    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        // let itemlists be sorted
//        itemListCraftable.rebuild(UIItemCatBar.FILTER_CAT_ALL)
        playerThings.rebuild(UIItemCatBar.FILTER_CAT_ALL)
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }

    init {
        addUIitem(playerThings)
        addUIitem(fireboxItemSlot)
        addUIitem(oreItemSlot)
        addUIitem(productItemslot)
    }

    override fun show() {
        super.show()
        playerThings.setGetInventoryFun { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = backdropColour
        batch.draw(smelterBackdrop, backdropX, backdropY, smelterBackdrop.regionWidth * 6f, smelterBackdrop.regionHeight * 6f)

        uiItems.forEach { it.render(frameDelta, batch, camera) }




        // control hints
        batch.color = Color.WHITE
        val controlHintXPos = leftPanelX + 2f
        blendNormalStraightAlpha(batch)
        App.fontGame.draw(batch, controlHelp, controlHintXPos, UIInventoryFull.yEnd - 20)


        if (INGAME.actorNowPlaying != null) {
            //draw player encumb
            val encumbBarXPos = playerThings.posX + playerThings.width - UIInventoryCells.weightBarWidth + 36
            val encumbBarYPos = UIInventoryFull.yEnd - 20 + 3f
            UIInventoryCells.drawEncumbranceBar(batch, encumbBarXPos, encumbBarYPos, encumbrancePerc, INGAME.actorNowPlaying!!.inventory)
        }


        blendNormalStraightAlpha(batch)
    }

    override fun dispose() {
    }




}