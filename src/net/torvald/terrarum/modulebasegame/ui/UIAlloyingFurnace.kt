package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureAlloyingFurnace
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemCatBar
import net.torvald.terrarum.ui.UIItemInventoryElemWide
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.concurrent.atomic.AtomicInteger

class UIAlloyingFurnace(smelter: FixtureAlloyingFurnace) : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
) {


    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private var clickedOnState = AtomicInteger(SmelterGuiEventBuilder.PRODUCT_SLOT) // Used to set inventory filter and its behaviour. 0: default, 1: oreslot, 2: firebox

    private val playerThings = UITemplateHalfInventory(this, false).also {
        it.itemListTouchDownFun = SmelterGuiEventBuilder.getPlayerSlotTouchDownFun(
            clickedOnState,
            smelter.fireboxItemStatus,
            listOf(smelter.oreItem1Status, smelter.oreItem2Status),
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
        it.itemListWheelFun = SmelterGuiEventBuilder.getPlayerSlotWheelFun(
            clickedOnState,
            smelter.fireboxItemStatus,
            listOf(smelter.oreItem1Status, smelter.oreItem2Status),
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
    }

    fun getPlayerInventory(): ActorInventory = INGAME.actorNowPlaying!!.inventory

    init {
        CommonResourcePool.addToLoadingList("basegame_gui_smelter_icons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/smelter_icons.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val smelterCellIcons = CommonResourcePool.getAsTextureRegionPack("basegame_gui_smelter_icons")

    private var smelterBackdrops =
        CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/alloying_furnace.tga")


    private val leftPanelWidth = playerThings.width
    private val leftPanelHeight = playerThings.height
    private val leftPanelX = playerThings.posX - leftPanelWidth - UIItemInventoryItemGrid.listGap - UIItemInventoryElemWide.height
    private val leftPanelY = playerThings.posY

    private val backdropColour = Color(0x999999_c8.toInt())
    private val backdropZoom = 6
    private val backdropX = (leftPanelX + (leftPanelWidth - smelterBackdrops.tileW * backdropZoom) / 2).toFloat()
    private val backdropY = (leftPanelY + (leftPanelHeight - smelterBackdrops.tileH * backdropZoom) / 2).toFloat()

    private val oreX1 = backdropX + 6 * backdropZoom + 6
    private val oreX2 = backdropX + 18 * backdropZoom + 6
    private val oreY = backdropY + 23 * backdropZoom + 3

    private val fireboxX = backdropX + 12 * backdropZoom + 6
    private val fireboxY = backdropY + 39 * backdropZoom + 3

    private val productX = backdropX + 37 * backdropZoom + 3
    private val productY = backdropY + 39 * backdropZoom + 3

    private val thermoX = (backdropX + 24 * backdropZoom + 1).toInt()
    private val thermoY = (backdropY + 39 * backdropZoom + 3).toInt()




    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        // let itemlists be sorted
        inventoryFilter = { _: InventoryPair -> true }
        playerThings.rebuild(UIItemCatBar.FILTER_CAT_ALL)
        encumbrancePerc = getPlayerInventory().encumberment.toFloat()
    }

    private var inventoryFilter = { _: InventoryPair -> true }

    private fun itemListUpdate(filter: (InventoryPair) -> Boolean) {
        // let itemlists be sorted
        inventoryFilter = filter
        playerThings.rebuild(filter)
        encumbrancePerc = getPlayerInventory().encumberment.toFloat()
    }

    private fun itemListUpdateKeepCurrentFilter() {
        // let itemlists be sorted
        playerThings.rebuild(inventoryFilter)
        encumbrancePerc = getPlayerInventory().encumberment.toFloat()
    }

    init {
        addUIitem(playerThings)
//        addUIitem(oreItemSlot)
//        addUIitem(fireboxItemSlot)
//        addUIitem(productItemslot)
    }

    override fun updateImpl(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

}
