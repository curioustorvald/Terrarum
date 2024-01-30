package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.colourutil.cieluv_getGradient
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemCatBar.Companion.FILTER_CAT_ALL
import net.torvald.terrarum.ui.UIItemInventoryElemWide.Companion.UNIQUE_ITEM_HAS_NO_AMOUNT
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC
import net.torvald.unicode.getMouseButton
import kotlin.math.roundToInt

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
                    if (smelter.oreItem == null) {
                        getPlayerInventory().remove(gameItem.dynamicID, amount)
                        smelter.oreItem = InventoryPair(gameItem.dynamicID, amount)
                    }
                    else if (smelter.oreItem!!.itm == gameItem.dynamicID) {
                        getPlayerInventory().remove(gameItem.dynamicID, amount)
                        smelter.oreItem!!.qty += amount
                    }
                }
                // firebox
                else if (clickedOn == 2) {
                    if (smelter.fireboxItem == null) {
                        getPlayerInventory().remove(gameItem.dynamicID, amount)
                        smelter.fireboxItem = InventoryPair(gameItem.dynamicID, amount)
                    }
                    else if (smelter.fireboxItem!!.itm == gameItem.dynamicID) {
                        getPlayerInventory().remove(gameItem.dynamicID, amount)
                        smelter.fireboxItem!!.qty += amount
                    }
                }
            }

            itemListUpdateKeepCurrentFilter()
        }
        it.itemListWheelFun = { gameItem, _, _, scrollY, _, _ ->
            val scrollY = -scrollY
            if (gameItem != null) {
                val playerInventory = getPlayerInventory()
                val addCount1 = scrollY.toLong()

                if (clickedOn == 1 && (smelter.oreItem == null || smelter.oreItem!!.itm == gameItem.dynamicID)) {
                    val itemToUse = smelter.oreItem?.itm ?: gameItem.dynamicID

                    val addCount2 = scrollY.toLong().coerceIn(
                        -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                        smelter.oreItem?.qty ?: 0L,
                    )

                    // add to the inventory slot
                    if (smelter.oreItem != null && addCount1 >= 1L) {
                        getPlayerInventory().add(smelter.oreItem!!.itm, addCount2)
                        smelter.oreItem!!.qty -= addCount2
                    }
                    // remove from the inventory slot
                    else if (addCount1 <= -1L) {
                        playerInventory.remove(itemToUse, -addCount2)
                        if (smelter.oreItem == null)
                            smelter.oreItem = InventoryPair(itemToUse, -addCount2)
                        else
                            smelter.oreItem!!.qty -= addCount2
                    }
                    if (smelter.oreItem != null && smelter.oreItem!!.qty == 0L) smelter.oreItem = null
                    else if (smelter.oreItem != null && smelter.oreItem!!.qty < 0L) throw Error("Item removal count is larger than what was on the slot")
                    itemListUpdateKeepCurrentFilter()
                }
                else if (clickedOn == 2 && (smelter.fireboxItem == null || smelter.fireboxItem!!.itm == gameItem.dynamicID)) {
                    val itemToUse = smelter.fireboxItem?.itm ?: gameItem.dynamicID

                    val addCount2 = scrollY.toLong().coerceIn(
                        -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                        smelter.fireboxItem?.qty ?: 0L,
                    )

                    // add to the inventory slot
                    if (smelter.fireboxItem != null && addCount1 >= 1L) {
                        getPlayerInventory().add(smelter.fireboxItem!!.itm, addCount2)
                        smelter.fireboxItem!!.qty -= addCount2
                    }
                    // remove from the inventory slot
                    else if (addCount1 <= -1L) {
                        playerInventory.remove(itemToUse, -addCount2)
                        if (smelter.fireboxItem == null)
                            smelter.fireboxItem = InventoryPair(itemToUse, -addCount2)
                        else
                            smelter.fireboxItem!!.qty -= addCount2
                    }
                    if (smelter.fireboxItem != null && smelter.fireboxItem!!.qty == 0L) smelter.fireboxItem = null
                    else if (smelter.fireboxItem != null && smelter.fireboxItem!!.qty < 0L) throw Error("Item removal count is larger than what was on the slot")
                    itemListUpdateKeepCurrentFilter()
                }
                else {
                    itemListUpdateKeepCurrentFilter()
                }
            }
        }
    }

    fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private var listModeButtonPushed = false

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

    private val oreX = backdropX + 12 * backdropZoom + 6
    private val oreY = backdropY + 23 * backdropZoom + 3

    private val fireboxX = backdropX + 12 * backdropZoom + 6
    private val fireboxY = backdropY + 39 * backdropZoom + 3

    private val productX = backdropX + 37 * backdropZoom + 3
    private val productY = backdropY + 39 * backdropZoom + 3

    private val thermoX = (backdropX + 24 * backdropZoom + 1).toInt()
    private val thermoY = (backdropY + 39 * backdropZoom + 3).toInt()

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
        },
        wheelFun = { _, _, _, scrollY, _, _ ->
            val scrollY = -scrollY
            if (clickedOn == 1 && smelter.oreItem != null) {
                val playerInventory = getPlayerInventory()
                val removeCount1 = scrollY.toLong()
                val removeCount2 = scrollY.toLong().coerceIn(
                    -smelter.oreItem!!.qty,
                    playerInventory.searchByID(smelter.oreItem!!.itm)?.qty ?: 0L,
                )

                // add to the slot
                if (removeCount1 >= 1L) {
                    playerInventory.remove(smelter.oreItem!!.itm, removeCount2)
                    smelter.oreItem!!.qty += removeCount2
                }
                // remove from the slot
                else if (removeCount1 <= -1L) {
                    getPlayerInventory().add(smelter.oreItem!!.itm, -removeCount2)
                    smelter.oreItem!!.qty += removeCount2
                }
                if (smelter.oreItem!!.qty == 0L) smelter.oreItem = null
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
        },
        wheelFun = { _, _, _, scrollY, _, _ ->
            val scrollY = -scrollY
            if (clickedOn == 2 && smelter.fireboxItem != null) {
                val playerInventory = getPlayerInventory()
                val removeCount1 = scrollY.toLong()
                val removeCount2 = scrollY.toLong().coerceIn(
                    -smelter.fireboxItem!!.qty,
                    playerInventory.searchByID(smelter.fireboxItem!!.itm)?.qty ?: 0L,
                )

                // add to the slot
                if (removeCount1 >= 1L) {
                    playerInventory.remove(smelter.fireboxItem!!.itm, removeCount2)
                    smelter.fireboxItem!!.qty += removeCount2
                }
                // remove from the slot
                else if (removeCount1 <= -1L) {
                    getPlayerInventory().add(smelter.fireboxItem!!.itm, -removeCount2)
                    smelter.fireboxItem!!.qty += removeCount2
                }
                if (smelter.fireboxItem!!.qty == 0L) smelter.fireboxItem = null
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
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = { _, _, button, _, self ->
            if (clickedOn != 0) {
                clickedOn = 0
                oreItemSlot.forceHighlighted = false
                fireboxItemSlot.forceHighlighted = false
                itemListUpdate()
            }

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
        },
        wheelFun = { _, _, _, scrollY, _, _ ->
            val scrollY = -scrollY
            if (smelter.productItem != null) {
                val removeCount1 = scrollY.toLong()
                val removeCount2 = scrollY.toLong().coerceIn(
                    -smelter.productItem!!.qty,
                    0L,
                )

                // remove from the slot
                if (removeCount1 <= -1L) {
                    getPlayerInventory().add(smelter.productItem!!.itm, -removeCount2)
                    smelter.productItem!!.qty += removeCount2
                }
                if (smelter.productItem!!.qty == 0L) smelter.productItem = null
                itemListUpdateKeepCurrentFilter()
            }
            else {
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

        tooltipShowing.clear()
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

        if (!oreItemSlot.mouseUp &&
            !fireboxItemSlot.mouseUp &&
            !productItemslot.mouseUp &&
            !playerThings.itemList.mouseUp &&
            !playerThings.itemList.navRemoCon.mouseUp
        ) {

            clickedOn = 0

            oreItemSlot.forceHighlighted = false
            fireboxItemSlot.forceHighlighted = false
            itemListUpdate()
        }

        return true
    }

    private val SP = "\u3000"
    private val ML = getMouseButton(App.getConfigInt("config_mouseprimary"))
    private val MR = getMouseButton(App.getConfigInt("config_mousesecondary"))
    private val MW = getMouseButton(2)
    private val controlHelpForSmelter = listOf(
        // no slot selected
        { if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$ML ${Lang["GAME_ACTION_SELECT_SLOT"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" },
        // ore slot
        { if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$ML ${Lang["GAME_ACTION_TAKE_ALL_CONT"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_TAKE_ONE_CONT"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" },
        // firebox slot
        { if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$ML ${Lang["GAME_ACTION_TAKE_ALL_CONT"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_TAKE_ONE_CONT"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" }
    )

    private val controlHelpForInventory = listOf(
        // no slot selected
        { "" },
        // ore slot
        { if (App.environment == RunningEnvironment.PC)
            "$ML ${Lang["GAME_ACTION_PUT_ALL_CONT"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_PUT_ONE_CONT"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" },
        // firebox slot
        { if (App.environment == RunningEnvironment.PC)
            "$ML ${Lang["GAME_ACTION_PUT_ALL_CONT"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_PUT_ONE_CONT"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" }
    )

    private val controlHelpForInventoryTwoRows = listOf(
        // no slot selected
        { "" },
        // ore slot
        { if (App.environment == RunningEnvironment.PC)
            "$ML ${Lang["GAME_ACTION_PUT_ALL"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_PUT_ONE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" },
        // firebox slot
        { if (App.environment == RunningEnvironment.PC)
            "$ML ${Lang["GAME_ACTION_PUT_ALL"]}$SP" +
            "$MW$MR ${Lang["GAME_ACTION_PUT_ONE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" }
    )


    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = backdropColour
        batch.draw(smelterBackdrop, backdropX, backdropY, smelterBackdrop.regionWidth * 6f, smelterBackdrop.regionHeight * 6f)

        uiItems.forEach { it.render(frameDelta, batch, camera) }

        drawProgressGauge(batch, oreItemSlot.posX, oreItemSlot.posY, smelter.progress)
        drawProgressGauge(batch, fireboxItemSlot.posX, fireboxItemSlot.posY, smelter.fuelCaloriesNow / (smelter.fuelCaloriesMax ?: Float.POSITIVE_INFINITY))
        drawThermoGauge(batch, thermoX, thermoY, smelter.temperature)


        // control hints
        batch.color = Color.WHITE
        val controlHintXPos = leftPanelX + 2f
        val controlHintXPos2 = playerThings.posX + 2f
        blendNormalStraightAlpha(batch)
        App.fontGame.draw(batch, controlHelpForSmelter[clickedOn](), controlHintXPos, UIInventoryFull.yEnd - 20)

        // deal with the text that is too long
        val encumbBarXPos = playerThings.posX + playerThings.width - UIInventoryCells.weightBarWidth + 36
        val encumbBarYPos = UIInventoryFull.yEnd - 20 + 3f

        val tr = controlHelpForInventory[clickedOn]()
        val trLen = App.fontGame.getWidth(tr)
        val encumbTextX = encumbBarXPos - 6 - App.fontGame.getWidth(Lang["GAME_INVENTORY_ENCUMBRANCE"])
        if (controlHintXPos2 + trLen + 4 >= encumbTextX) {
            controlHelpForInventoryTwoRows[clickedOn]().split(SP).forEachIndexed { index, s ->
                App.fontGame.draw(batch, s, controlHintXPos2, UIInventoryFull.yEnd - 20 + 20 * index)
            }
        }
        else {
            App.fontGame.draw(batch, controlHelpForInventory[clickedOn](), controlHintXPos2, UIInventoryFull.yEnd - 20)
        }



        if (INGAME.actorNowPlaying != null) {
            //draw player encumb
            UIInventoryCells.drawEncumbranceBar(batch, encumbBarXPos, encumbBarYPos, encumbrancePerc, INGAME.actorNowPlaying!!.inventory)
        }


        blendNormalStraightAlpha(batch)
    }

    private val colProgress = Color(0xbbbbbbff.toInt())
    private val colTemp1 = Color(0x99000bff.toInt())
    private val colTemp2 = Color(0xffe200ff.toInt())



    /**
     * @param x x-position of the inventory cell that will have the gauge
     * @param y y-position of the inventory cell that will have the gauge
     */
    private fun drawProgressGauge(batch: SpriteBatch, x: Int, y: Int, percentage: Float) {
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, x - 7, y, 6, UIItemInventoryElemSimple.height)

        batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
        Toolkit.drawStraightLine(batch, x - 7, y - 1, x - 1, 1, false)
        Toolkit.drawStraightLine(batch, x - 7, y + UIItemInventoryElemSimple.height, x - 1, 1, false)
        Toolkit.drawStraightLine(batch, x - 8, y, y + UIItemInventoryElemSimple.height, 1, true)

        batch.color = colProgress
        Toolkit.fillArea(batch, x - 7, y + UIItemInventoryElemSimple.height, 6, -(percentage * UIItemInventoryElemSimple.height).roundToInt())
    }

    private fun drawThermoGauge(batch: SpriteBatch, x: Int, y: Int, percentage: Float) {
        batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
        Toolkit.drawStraightLine(batch, x, y - 1, x + 4, 1, false)
        Toolkit.drawStraightLine(batch, x, y + UIItemInventoryElemSimple.height + 7, x + 4, 1, false)

        Toolkit.drawStraightLine(batch, x - 1, y, y + UIItemInventoryElemSimple.height, 1, true)
        Toolkit.drawStraightLine(batch, x + 4, y, y + UIItemInventoryElemSimple.height, 1, true)
        Toolkit.drawStraightLine(batch, x - 1, y + UIItemInventoryElemSimple.height + 6, y + UIItemInventoryElemSimple.height + 7, 1, true)
        Toolkit.drawStraightLine(batch, x + 4, y + UIItemInventoryElemSimple.height + 6, y + UIItemInventoryElemSimple.height + 7, 1, true)

        Toolkit.drawStraightLine(batch, x - 2, y + UIItemInventoryElemSimple.height, y + UIItemInventoryElemSimple.height + 1, 1, true)
        Toolkit.drawStraightLine(batch, x + 5, y + UIItemInventoryElemSimple.height, y + UIItemInventoryElemSimple.height + 1, 1, true)
        Toolkit.drawStraightLine(batch, x - 2, y + UIItemInventoryElemSimple.height + 5, y + UIItemInventoryElemSimple.height + 6, 1, true)
        Toolkit.drawStraightLine(batch, x + 5, y + UIItemInventoryElemSimple.height + 5, y + UIItemInventoryElemSimple.height + 6, 1, true)

        Toolkit.drawStraightLine(batch, x - 3, y + UIItemInventoryElemSimple.height + 1, y + UIItemInventoryElemSimple.height + 5, 1, true)
        Toolkit.drawStraightLine(batch, x + 6, y + UIItemInventoryElemSimple.height + 1, y + UIItemInventoryElemSimple.height + 5, 1, true)


        batch.color = cieluv_getGradient(percentage, colTemp1, colTemp2)
        Toolkit.fillArea(batch, x, y + UIItemInventoryElemSimple.height, 4, -(percentage * UIItemInventoryElemSimple.height).roundToInt())

        Toolkit.fillArea(batch, x, y + UIItemInventoryElemSimple.height, 4, 6)
        Toolkit.fillArea(batch, x - 1, y + UIItemInventoryElemSimple.height + 1, 6, 4)
    }



    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.disablePlayerControl()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resumePlayerControl()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun dispose() {
    }

}