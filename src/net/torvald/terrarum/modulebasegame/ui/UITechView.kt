package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarum.ui.UIItemInventoryElemSimple
import net.torvald.unicode.getKeycapPC
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-02-18.
 */
class UITechView(val inventoryUI: UIInventoryFull?, val parentContainer: UICrafting, private val colourTheme: InventoryCellColourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme,
) : UICanvas(
    toggleKeyLiteral = if (inventoryUI == null) "control_key_inventory" else null,
    toggleButtonLiteral = if (inventoryUI == null) "control_gamepad_start" else null
) {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val verticalCells = UIInventoryFull.CELLS_VRT
    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2

    private val posX0 = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val posY0 = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

    private val posX1 = posX0 - UIItemListNavBarVertical.LIST_TO_CONTROL_GAP - UIItemListNavBarVertical.WIDTH - 4
    private val posY1 = posY0

    private val panelX = 32 + parentContainer.transitionalCraftingUI.itemListCraftable.navRemoCon.posX + 12
    private val panelY = parentContainer.transitionalCraftingUI.itemListCraftable.navRemoCon.posY - 8
    private val panelWidth = 720
    private val panelHeight = parentContainer.transitionalCraftingUI.itemListCraftable.height +
            parentContainer.transitionalCraftingUI.itemListIngredients.height + 64 // 64 is a magic number

    private val navbarWidth = UIItemListNavBarVertical.WIDTH
    private val navbarHeight = 82 // a magic number
    private val navbarX = panelX - 32 // also a magic number
    private val navbarY = panelY + panelHeight - navbarHeight

    private val fakeNavbarY = parentContainer.transitionalCraftingUI.itemListIngredients.posY

    private val catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    private val menuButtonTechView = UIItemImageButton(
        this, catIcons.get(20, 1),
        initialX = navbarX,
        initialY = getIconPosY(0),
        activeCol = Toolkit.Theme.COL_SELECTED,
        inactiveCol = Toolkit.Theme.COL_SELECTED,
        highlightable = false
    )
    private val menuButtonCraft = UIItemImageButton(
        this, catIcons.get(19, 1),
        initialX = navbarX,
        initialY = getIconPosY(1),
        highlightable = true
    ).also {
        it.clickOnceListener = { _, _ ->
            parentContainer.showCraftingUI()
            it.highlighted = false
        }
    }

    fun getIconPosY(index: Int) = (fakeNavbarY + ((index*2+1)/4f) * navbarHeight).roundToInt() - catIcons.tileH/2

    init {
        addUIitem(menuButtonCraft)
        addUIitem(menuButtonTechView)
    }
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset

    private val cellHighlightNormalCol2 = colourTheme.cellHighlightNormalCol.cpy().also { it.a /= 2f }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // draw fake navbar //
        // draw the tray
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, navbarX - 4, navbarY, navbarWidth, navbarHeight)
        // cell border
        batch.color = colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, navbarX - 4, navbarY, navbarWidth, navbarHeight)
        // cell divider
        batch.color = cellHighlightNormalCol2
        Toolkit.drawStraightLine(batch, navbarX - 2, navbarY + navbarHeight/2, navbarX-2 + navbarWidth-4, 1, false)



        // draw window //
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, panelX, panelY, panelWidth, panelHeight)
        // cell border
        batch.color = colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, panelX, panelY, panelWidth, panelHeight)


        uiItems.forEach { it.render(frameDelta, batch, camera) }

        // control hints
        val controlHintXPos = thisOffsetX + 2f
        blendNormalStraightAlpha(batch)
        batch.color = Color.WHITE
        App.fontGame.draw(batch, controlHelp, controlHintXPos, UIInventoryFull.yEnd - 20)

    }


    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}\u3000 " +
                    "${App.gamepadLabelLEFTRIGHT} ${Lang["GAME_OBJECTIVE_MULTIPLIER"]}\u3000 " +
                    "${App.gamepadLabelWest} ${Lang["GAME_ACTION_CRAFT"]}"


    override fun updateImpl(delta: Float) {
    }

    override fun doOpening(delta: Float) {
        handler.opacity = 1f
    }

    override fun doClosing(delta: Float) {
        handler.opacity = 1f
    }

    override fun endOpening(delta: Float) {
        handler.opacity = 1f
    }

    override fun endClosing(delta: Float) {
        handler.opacity = 1f
    }

    override fun dispose() {
    }

    fun resetUI() {
    }
}