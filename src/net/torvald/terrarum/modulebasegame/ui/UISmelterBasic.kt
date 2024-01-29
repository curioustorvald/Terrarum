package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemCatBar.Companion.FILTER_CAT_ALL
import net.torvald.terrarum.ui.UIItemInventoryElemWide.Companion.UNIQUE_ITEM_HAS_NO_AMOUNT
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Created by minjaesong on 2024-01-29.
 */
class UISmelterBasic(val smelter: FixtureSmelterBasic) : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
) {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val playerThings = UITemplateHalfInventory(this, false).also {
        it.itemListTouchDownFun = { gameItem, amount, button, _, _ ->
            val amount = if (button == App.getConfigInt("config_mouseprimary"))
                amount
            else if (button == App.getConfigInt("config_mousesecondary"))
                1
            else
                null

            // oreslot
            if (amount != null && gameItem != null) {
                if (clickedOn == 1) {
                    getPlayerInventory().remove(gameItem.dynamicID, amount)

                    if (smelter.oreItem == null)
                        smelter.oreItem = InventoryPair(gameItem.dynamicID, amount)
                    else
                        smelter.oreItem!!.qty += amount
                }
                // firebox
                else if (clickedOn == 2) {
                    getPlayerInventory().remove(gameItem.dynamicID, amount)

                    if (smelter.fireboxItem == null)
                        smelter.fireboxItem = InventoryPair(gameItem.dynamicID, amount)
                    else
                        smelter.fireboxItem!!.qty += amount
                }
            }

            itemListUpdateKeepCurrentFilter()
        }
    }

    fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    init {
        CommonResourcePool.addToLoadingList("basegame_gui_smelter_icons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/smelter_icons.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val smelterCellIcons = CommonResourcePool.getAsTextureRegionPack("basegame_gui_smelter_icons")

    private var smelterBackdrop =
        FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_tall.tga")


    private val leftPanelWidth = playerThings.width
    private val leftPanelHeight = playerThings.height
    private val leftPanelX = playerThings.posX - leftPanelWidth - UIItemInventoryItemGrid.listGap - UIItemInventoryElemWide.height
    private val leftPanelY = playerThings.posY

    private val backdropColour = Color(0x999999_c8.toInt())
    private val backdropZoom = 6
    private val backdropX = (leftPanelX + (leftPanelWidth - smelterBackdrop.regionWidth * backdropZoom) / 2).toFloat()
    private val backdropY = (leftPanelY + (leftPanelHeight - smelterBackdrop.regionHeight * backdropZoom) / 2).toFloat()

    private val oreX = backdropX + 12 * backdropZoom + 3
    private val oreY = backdropY + 23 * backdropZoom + 3

    private val fireboxX = backdropX + 12 * backdropZoom + 3
    private val fireboxY = backdropY + 39 * backdropZoom + 3

    private val productX = backdropX + 37 * backdropZoom + 3
    private val productY = backdropY + 39 * backdropZoom + 3

    /*
    Click on the button when item is there:
    ButtonPrimary: take all the item
    ButtonSecondary: take only one item (or show radial menu?)

    Click on the button when item is not there:
    ButtonPrimary: apply filter to the inventory

    Click on the area that has no UIitem:
    ButtonPrimary: reset the inventory filter

    Click on the inventory list:
    ButtonPrimary: use all the item
    ButtonSecondary: use only one item
     */

    private var clickedOn = 0 // Used to set inventory filter and its behaviour. 0: default, 1: oreslot, 2: firebox

    private val oreItemSlot: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, oreX.toInt(), oreY.toInt(),
        updateOnNull = true,
        emptyCellIcon = smelterCellIcons.get(1, 1),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { _, _, button, _, self ->
            if (clickedOn != 1) {
                clickedOn = 1
                self.forceHighlighted = true
                fireboxItemSlot.forceHighlighted = false
                itemListUpdate { ItemCodex.hasTag(it.itm, "SMELTABLE") }
            }
            else if (smelter.oreItem != null) {
                val removeCount = if (button == App.getConfigInt("config_mouseprimary"))
                    smelter.oreItem!!.qty
                else if (button == App.getConfigInt("config_mousesecondary"))
                    1L
                else
                    null

                if (removeCount != null) {
                    getPlayerInventory().add(smelter.oreItem!!.itm, removeCount)
                    smelter.oreItem!!.qty -= removeCount
                    if (smelter.oreItem!!.qty == 0L) smelter.oreItem = null
                }
                itemListUpdateKeepCurrentFilter()
            }
            else {
                itemListUpdateKeepCurrentFilter()
            }
        }
    )
    private val fireboxItemSlot: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, fireboxX.toInt(), fireboxY.toInt(),
        emptyCellIcon = smelterCellIcons.get(0, 0),
        updateOnNull = true,
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { _, _, button, _, self ->
            if (clickedOn != 2) {
                clickedOn = 2
                self.forceHighlighted = true
                oreItemSlot.forceHighlighted = false
                itemListUpdate { ItemCodex.hasTag(it.itm, "BURNABLE") }
            }
            else if (smelter.fireboxItem != null) {
                val removeCount = if (button == App.getConfigInt("config_mouseprimary"))
                    smelter.fireboxItem!!.qty
                else if (button == App.getConfigInt("config_mousesecondary"))
                    1L
                else
                    null

                if (removeCount != null) {
                    getPlayerInventory().add(smelter.fireboxItem!!.itm, removeCount)
                    smelter.fireboxItem!!.qty -= removeCount
                    if (smelter.fireboxItem!!.qty == 0L) smelter.fireboxItem = null
                }
                itemListUpdateKeepCurrentFilter()
            }
            else {
                itemListUpdateKeepCurrentFilter()
            }
        }
    )
    private val productItemslot: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, productX.toInt(), productY.toInt(),
        emptyCellIcon = smelterCellIcons.get(1, 0),
        updateOnNull = true,
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { _, _, button, _, self ->
            if (smelter.productItem != null) {
                val removeCount = if (button == App.getConfigInt("config_mouseprimary"))
                    smelter.productItem!!.qty
                else if (button == App.getConfigInt("config_mousesecondary"))
                    1L
                else
                    null

                if (removeCount != null) {
                    getPlayerInventory().add(smelter.productItem!!.itm, removeCount)
                    smelter.productItem!!.qty -= removeCount
                    if (smelter.productItem!!.qty == 0L) smelter.productItem = null
                }
                itemListUpdateKeepCurrentFilter()
            }
        }
    )

    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        // let itemlists be sorted
        inventoryFilter = { _: InventoryPair -> true }
        playerThings.rebuild(FILTER_CAT_ALL)
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }
    
    private var inventoryFilter = { _: InventoryPair -> true }
    
    private fun itemListUpdate(filter: (InventoryPair) -> Boolean) {
        // let itemlists be sorted
        inventoryFilter = filter
        playerThings.rebuild(filter)
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }

    private fun itemListUpdateKeepCurrentFilter() {
        // let itemlists be sorted
        playerThings.rebuild(inventoryFilter)
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }

    init {
        addUIitem(playerThings)
        addUIitem(oreItemSlot)
        addUIitem(fireboxItemSlot)
        addUIitem(productItemslot)
    }

    override fun show() {
        super.show()

        clickedOn = 0
        oreItemSlot.forceHighlighted = false
        fireboxItemSlot.forceHighlighted = false

        playerThings.setGetInventoryFun { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }

        oreItemSlot.item = ItemCodex[smelter.oreItem?.itm]
        oreItemSlot.itemImage = ItemCodex.getItemImage(smelter.oreItem?.itm)
        oreItemSlot.amount = smelter.oreItem?.qty ?: UNIQUE_ITEM_HAS_NO_AMOUNT

        fireboxItemSlot.item = ItemCodex[smelter.fireboxItem?.itm]
        fireboxItemSlot.itemImage = ItemCodex.getItemImage(smelter.fireboxItem?.itm)
        fireboxItemSlot.amount = smelter.fireboxItem?.qty ?: UNIQUE_ITEM_HAS_NO_AMOUNT

        productItemslot.item = ItemCodex[smelter.productItem?.itm]
        productItemslot.itemImage = ItemCodex.getItemImage(smelter.productItem?.itm)
        productItemslot.amount = smelter.productItem?.qty ?: UNIQUE_ITEM_HAS_NO_AMOUNT
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        if (!oreItemSlot.mouseUp && !fireboxItemSlot.mouseUp && !productItemslot.mouseUp && !playerThings.itemList.mouseUp) {
            clickedOn = 0
            oreItemSlot.forceHighlighted = false
            fireboxItemSlot.forceHighlighted = false
            itemListUpdate()
        }

        return true
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