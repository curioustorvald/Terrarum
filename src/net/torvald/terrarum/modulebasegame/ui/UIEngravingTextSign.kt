package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-03-22.
 */
class UIEngravingTextSign : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
) {

    init {
        CommonResourcePool.addToLoadingList("spritesheet:copper_sign") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/text_sign_glass_copper.tga"), TILE_SIZE, TILE_SIZE)
        }
        CommonResourcePool.loadAll()
    }

    private val signSheet = CommonResourcePool.getAsTextureRegionPack("spritesheet:copper_sign")

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    fun getPlayerInventory(): ActorInventory = INGAME.actorNowPlaying!!.inventory

    private val row0 = App.scr.halfh + 48
    private val row1 = row0 + 60
    private val row2 = row1 + 40

    private val row3 = maxOf(UIInventoryFull.yEnd.toInt() - 20 - 36, row2 + 64)

    private val goButtonWidth = 180

    private val internalWidth = 480
    private val drawX = (Toolkit.drawWidth - internalWidth) / 2
    private val inputWidth = 350
    private val spinnerWidth = ControlPanelCommon.CONFIG_SPINNER_WIDTH
    private val ingredientsWidth = UIItemInventoryElemSimple.height * 3 + 16
    private val inputX = drawX + internalWidth - inputWidth + 5

    private val textInput = UIItemTextLineInput(this, inputX, row0, inputWidth)

    private val COPPER_BULB = "item@basegame:35"
    private val ROCK_TILE = Block.STONE_TILE_WHITE
    private val GLASS = Block.GLASS_CRUDE

    private fun setIngredient(num: Int) {
        ingredients.clear()
        ingredients.add(GLASS, num * 2L)
        ingredients.add(ROCK_TILE, num * 1L)
        ingredients.add(COPPER_BULB, num * 1L)
        ingredientsPanel.rebuild(UIItemCatBar.FILTER_CAT_ALL)
    }

    private fun refreshCraftButtonStatus() {
        val player = getPlayerInventory()
        val canCraft = ingredients.all {
            (player.searchByID(it.itm)?.qty ?: 0L) >= it.qty
        }
        goButton.isEnabled = canCraft
    }

    private val ingredients = FixtureInventory() // this one is definitely not to be changed
    private val ingredientsPanel = UIItemInventoryItemGrid(
        this,
        { ingredients },
        drawX,
        row2,
        3, 1,
        drawScrollOnRightside = false,
        drawWallet = false,
        hideSidebar = true,
        colourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme.copy(
            cellHighlightSubCol = Toolkit.Theme.COL_INACTIVE
        ),
        keyDownFun = { _, _, _, _, _ -> },
        wheelFun = { _, _, _, _, _, _ -> },
        touchDownFun = { _, _, _, _, _ -> },
    ).also {
        it.isCompactMode = true
    }

    private val panelCountSpinner = UIItemSpinner(this, inputX + inputWidth - ingredientsWidth, row2 - 1, 2, 2, 32, 1, spinnerWidth, numberToTextFunction = { "${it.toDouble().roundToInt()}" }).also {
        it.selectionChangeListener = { num0 ->
            val num = num0.toInt()
            setIngredient(num)
            refreshCraftButtonStatus()
        }
    }


    private val goButton = UIItemTextButton(this,
        { Lang["GAME_ACTION_CRAFT"] }, (width - goButtonWidth) / 2, row3, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->



        }
    }

    private fun resetUI() {
        panelCountSpinner.resetToSmallest()
        textInput.clearText()
        setIngredient(panelCountSpinner.value.toInt())
        refreshCraftButtonStatus()
    }

    override fun show() {
        super.show()
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
        resetUI()
    }

    init {
        addUIitem(textInput)
        addUIitem(panelCountSpinner)
        addUIitem(ingredientsPanel)
        addUIitem(goButton)
    }

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, opacity)

        // paint UI elements
        uiItems.forEach { it.render(frameDelta, batch, camera) }

        // paint labels
        batch.color = Color.WHITE

        val controlHintXPos = thisOffsetX + 2f
        blendNormalStraightAlpha(batch)
        App.fontGame.draw(batch, controlHelp, controlHintXPos, UIInventoryFull.yEnd - 20)

        App.fontGame.draw(batch, Lang["CONTEXT_ENGRAVER_TEXT"], drawX, row0)
        Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_OPTIONS_SIZE"], spinnerWidth, panelCountSpinner.posX, row1)
        Toolkit.drawTextCentered(batch, App.fontGame, Lang["GAME_INVENTORY_INGREDIENTS"], ingredientsWidth, ingredientsPanel.posX, row1)

    }

    override fun dispose() {

    }


    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.setTooltipMessage(null)
        INGAME.pause()
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.setTooltipMessage(null)
        INGAME.resume()
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        resetUI()
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

}