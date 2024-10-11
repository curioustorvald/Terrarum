package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.colourutil.cieluv_getGradient
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureAlloyingFurnace
import net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC
import net.torvald.unicode.getMouseButton
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class UIAlloyingFurnace(val smelter: FixtureAlloyingFurnace) : UICanvas(
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
    private val backdropZoom = 10f
    private val backdropX = (leftPanelX + (leftPanelWidth - smelterBackdrops.tileW * backdropZoom) / 2).toFloat()
    private val backdropY = (leftPanelY + (leftPanelHeight - smelterBackdrops.tileH * backdropZoom) / 2).toFloat()

    private val oreX1 = backdropX + 3 * backdropZoom + 7
    private val oreX2 = backdropX + 9 * backdropZoom + 2
    private val oreY1 = backdropY + 6 * backdropZoom + 3
    private val oreY2 = backdropY + 6 * backdropZoom + 3

    private val fireboxX = backdropX + 6 * backdropZoom + 4
    private val fireboxY = backdropY + 21 * backdropZoom - 2

    private val productX = backdropX + 21 * backdropZoom + 1
    private val productY = backdropY + 11 * backdropZoom - 3

    private val thermoX = (backdropX + 13 * backdropZoom + 3).toInt()
    private val thermoY = (backdropY + 21 * backdropZoom - 2).toInt()


    private val oreItemSlot1: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, oreX1.toInt(), oreY1.toInt(),
        updateOnNull = true,
        emptyCellIcon = smelterCellIcons.get(1, 1),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = SmelterGuiEventBuilder.getOreItemSlotTouchDownFun(
            clickedOnState,
            { listOf(fireboxItemSlot, oreItemSlot2) },
            playerThings,
            smelter.oreItem1Status, 0,
            { ItemCodex.hasAnyTagOf(it, "INGOT") && ItemCodex.hasNoTagOf(it, "ALLOY") },
            { getPlayerInventory() },
            { filter -> itemListUpdate(filter) },
            { itemListUpdateKeepCurrentFilter() }
        ),
        wheelFun = SmelterGuiEventBuilder.getOreItemSlotWheelFun(
            clickedOnState,
            smelter.oreItem1Status, 0,
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
    )
    private val oreItemSlot2: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, oreX2.toInt(), oreY2.toInt(),
        updateOnNull = true,
        emptyCellIcon = smelterCellIcons.get(1, 1),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = SmelterGuiEventBuilder.getOreItemSlotTouchDownFun(
            clickedOnState,
            { listOf(fireboxItemSlot, oreItemSlot1) },
            playerThings,
            smelter.oreItem2Status, 1,
            { ItemCodex.hasAnyTagOf(it, "INGOT") && ItemCodex.hasNoTagOf(it, "ALLOY") },
            { getPlayerInventory() },
            { filter -> itemListUpdate(filter) },
            { itemListUpdateKeepCurrentFilter() }
        ),
        wheelFun = SmelterGuiEventBuilder.getOreItemSlotWheelFun(
            clickedOnState,
            smelter.oreItem2Status, 1,
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
    )
    private val fireboxItemSlot: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, fireboxX.toInt(), fireboxY.toInt(),
        emptyCellIcon = smelterCellIcons.get(0, 0),
        updateOnNull = true,
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = SmelterGuiEventBuilder.getFireboxItemSlotTouchDownFun(
            clickedOnState,
            { listOf(oreItemSlot1, oreItemSlot2) },
            playerThings,
            smelter.fireboxItemStatus,
            { getPlayerInventory() },
            { filter -> itemListUpdate(filter) },
            { itemListUpdateKeepCurrentFilter() }
        ),
        wheelFun = SmelterGuiEventBuilder.getFireboxItemSlotWheelFun(
            clickedOnState,
            smelter.fireboxItemStatus,
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
    )
    private val productItemslot: UIItemInventoryElemSimple = UIItemInventoryElemSimple(
        this, productX.toInt(), productY.toInt(),
        emptyCellIcon = smelterCellIcons.get(1, 0),
        keyDownFun = { _, _, _, _, _ -> },
        touchDownFun = SmelterGuiEventBuilder.getProductItemSlotTouchDownFun(
            clickedOnState,
            { listOf(oreItemSlot1, oreItemSlot2, fireboxItemSlot) },
            playerThings,
            smelter.productItemStatus,
            { getPlayerInventory() },
            { itemListUpdate() },
            { itemListUpdateKeepCurrentFilter() }
        ),
        wheelFun = SmelterGuiEventBuilder.getProductItemSlotWheelFun(
            smelter.productItemStatus,
            { getPlayerInventory() },
            { itemListUpdateKeepCurrentFilter() }
        )
    )


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
        addUIitem(oreItemSlot1)
        addUIitem(oreItemSlot2)
        addUIitem(fireboxItemSlot)
        addUIitem(productItemslot)
    }

    override fun show() {
        super.show()

        clickedOnState.set(SmelterGuiEventBuilder.PRODUCT_SLOT)
        oreItemSlot1.forceHighlighted = false
        oreItemSlot2.forceHighlighted = false
        fireboxItemSlot.forceHighlighted = false

        playerThings.setGetInventoryFun { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()
    }


    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }

        oreItemSlot1.item = ItemCodex[smelter.oreItem1?.itm]
        oreItemSlot1.itemImage = ItemCodex.getItemImage(smelter.oreItem1?.itm)
        oreItemSlot1.amount = smelter.oreItem1?.qty ?: UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT

        oreItemSlot2.item = ItemCodex[smelter.oreItem2?.itm]
        oreItemSlot2.itemImage = ItemCodex.getItemImage(smelter.oreItem2?.itm)
        oreItemSlot2.amount = smelter.oreItem2?.qty ?: UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT

        fireboxItemSlot.item = ItemCodex[smelter.fireboxItem?.itm]
        fireboxItemSlot.itemImage = ItemCodex.getItemImage(smelter.fireboxItem?.itm)
        fireboxItemSlot.amount = smelter.fireboxItem?.qty ?: UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT

        productItemslot.item = ItemCodex[smelter.productItem?.itm]
        productItemslot.itemImage = ItemCodex.getItemImage(smelter.productItem?.itm)
        productItemslot.amount = smelter.productItem?.qty ?: UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        // unhighlight all cells when clicked outside
        if (!oreItemSlot1.mouseUp &&
            !oreItemSlot2.mouseUp &&
            !fireboxItemSlot.mouseUp &&
            !productItemslot.mouseUp &&
            !playerThings.itemList.mouseUp &&
            !playerThings.itemList.navRemoCon.mouseUp
        ) {

            clickedOnState.set(SmelterGuiEventBuilder.PRODUCT_SLOT)

            oreItemSlot1.forceHighlighted = false
            oreItemSlot2.forceHighlighted = false
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


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, opacity)

        val clickedOn = clickedOnState.get() / SmelterGuiEventBuilder.SLOT_INDEX_STRIDE


        batch.color = backdropColour
//        batch.draw(smelterBackdrops.get(1,0), backdropX, backdropY, smelterBackdrops.tileW * 6f, smelterBackdrops.tileH * 6f)
//        batch.color = backdropColour mul Color(1f, 1f, 1f, smelter.temperature)
        batch.draw(smelterBackdrops.get(0,0), backdropX, backdropY, smelterBackdrops.tileW * backdropZoom, smelterBackdrops.tileH * backdropZoom)

        uiItems.forEach { it.render(frameDelta, batch, camera) }

        drawProgressGauge(batch, oreItemSlot1.posX, oreItemSlot1.posY, smelter.progress / FixtureSmelterBasic.CALORIES_PER_ROASTING)
        drawProgressGauge(batch, fireboxItemSlot.posX, fireboxItemSlot.posY, (smelter.fuelCaloriesNow / (smelter.fuelCaloriesMax ?: Double.POSITIVE_INFINITY)).toFloat())
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
        val percentage = percentage.coerceIn(0f, 1f)

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
        val percentage = percentage.coerceIn(0f, 1f)

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
    }

    override fun dispose() {
    }

}
