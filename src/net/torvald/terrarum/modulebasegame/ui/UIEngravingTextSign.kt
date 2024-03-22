package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.itemproperties.Item
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameitems.ItemTextSignCopper
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.TIMES
import net.torvald.unicode.getKeycapPC
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-03-22.
 */
class UIEngravingTextSign : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
) {

    private var panelZoom = 4f
    init {
        CommonResourcePool.addToLoadingList("spritesheet:copper_sign") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/text_sign_glass_copper.tga"), TILE_SIZE, 2*TILE_SIZE)
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
    private val inputX = drawX + internalWidth - inputWidth

    private val textInput = UIItemTextLineInput(this, inputX - 1, row0, inputWidth).also {
        it.textCommitListener = { text ->
            val textLen = App.fontGame.getWidth(text) + 4
            val panelCountMin = (textLen / TILE_SIZEF).ceilToInt()

            updateMinimumLen(panelCountMin)
            panelCount = panelCountSpinner.value.toInt()

            updatePanelText(text, panelCount)
        }
    }

    private val COPPER_BULB = "item@basegame:35"
    private val ROCK_TILE = Block.STONE_TILE_WHITE
    private val GLASS = Block.GLASS_CRUDE


    private val backdropColour = Color(0xffffff_c8.toInt())

    private var fboText = FrameBuffer(Pixmap.Format.RGBA8888, 1, 1, false)
    private var fboBatch = SpriteBatch()
    private var fboCamera = OrthographicCamera(1f, 1f)

    private fun updateMinimumLen(mlen0: Int) {
        val mlen = mlen0.coerceAtLeast(2).toDouble()
        val delta = maxOf(panelCount.toDouble(), mlen) - panelCount.toDouble()
        panelCountSpinner.changeValueBy(delta.toInt())
        setIngredient(panelCountSpinner.value.toInt())
    }

    private fun updatePanelText(text: String, panelCount: Int) {
        fboText.dispose()
        fboText = FrameBuffer(Pixmap.Format.RGBA8888, panelCount*TILE_SIZE, 2*TILE_SIZE, false)
        fboText.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fboCamera.setToOrtho(true, panelCount*TILE_SIZEF, 2*TILE_SIZEF)

        fboText.inAction(fboCamera, fboBatch) {
            gdxClearAndEnableBlend(Color.CLEAR)
            fboBatch.color = Color.WHITE
            fboBatch.inUse { batch ->
                blendNormalStraightAlpha(batch)
                val tw = App.fontGame.getWidth(text)
                App.fontGame.draw(batch, text, 1 + (it.width - tw) / 2, 2)


                blendAlphaMask(batch)
                for (p in 0 until panelCount) {
                    batch.draw(signSheet.get(3, 0), TILE_SIZEF * p, 0f)
                }
            }
        }
    }

    private fun drawPanels(batch: SpriteBatch, xStart: Float, yStart: Float, panelCount: Int) {
        blendNormalStraightAlpha(batch)

        // panels
        batch.color = backdropColour
        for (p in 0 until panelCount) {
            val sprite = signSheet.get(if (p == 0) 0 else if (p == panelCount - 1) 2 else 1, 0)
            batch.draw(sprite, xStart + p * sprite.regionWidth * panelZoom, yStart, sprite.regionWidth * panelZoom, sprite.regionHeight * panelZoom)
        }

        // text
        batch.draw(fboText.colorBufferTexture, xStart, yStart, fboText.width * panelZoom, fboText.height * panelZoom)
    }

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

    private val panelCountSpinner = UIItemSpinner(
        this,
        inputX + inputWidth - ingredientsWidth + 19,
        row2 - 1,
        2, 2, 32, 1,
        spinnerWidth,
        numberToTextFunction = { "${it.toDouble().roundToInt()}" }
    ).also {
        it.selectionChangeListener = { num0 ->
            val num = num0.toInt()
            setIngredient(num)
            refreshCraftButtonStatus()
        }
    }


    private val resetButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_RESET"] }, width / 2 - 24 - goButtonWidth, row3, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            resetUI()
        }
    }

    private val goButton = UIItemTextButton(this,
        { Lang["GAME_ACTION_CRAFT"] }, width / 2 + 24, row3, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            val actorInventory = getPlayerInventory()
            val text = textInput.getText()
            val item = ItemTextSignCopper(Item.COPPER_SIGN).makeDynamic(actorInventory).also {
                it.extra["signContent"] = text
                it.extra["signPanelCount"] = panelCount
                it.nameSecondary = "[$panelCount${TIMES}2] $text"
            }

            actorInventory.add(item)

            resetUI()
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
        addUIitem(resetButton)
    }

    private var panelCount = panelCountSpinner.value.toInt()
    override fun updateImpl(delta: Float) {
        panelCount = panelCountSpinner.value.toInt()

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

        // paint preview
        drawPanels(batch, (width - panelCount * panelZoom * TILE_SIZE).toInt().div(2).toFloat(), height / 4f - 8, panelCount)

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
        fboText.tryDispose()
        fboBatch.tryDispose()
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