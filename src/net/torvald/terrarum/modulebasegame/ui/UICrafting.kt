package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Second
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.*
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-03-02.
 */
class UICrafting(val full: UIInventoryFull?) : UICanvas(
    toggleKeyLiteral = if (full == null) "control_key_inventory" else null,
    toggleButtonLiteral = if (full == null) "control_gamepad_start" else null
) {

    companion object {
        internal val LAST_LINE_IN_GRID = ((UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 2)) + 22//359 // TEMPORARY VALUE!

        val panelToggleBarY = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() + LAST_LINE_IN_GRID
        val panelToggleBarWidth = UIItemListNavBarVertical.WIDTH
        val panelToggleBarHeight = 82 // a magic number
        val catIconHeight = CommonResourcePool.getAsTextureRegionPack("inventory_category").tileH
    }

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    internal val transitionalCraftingUI = UICraftingWorkbench(full, this)
    internal val transitionalTechtreePanel = UITechView(full, this)
    val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - UIInventoryFull.internalWidth) / 2,
        UIInventoryFull.INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        0f,
        listOf(transitionalCraftingUI, transitionalTechtreePanel),
        listOf()
    )

    private val uis = listOf(transitionalCraftingUI, transitionalTechtreePanel)

    val panelToggleBarX = transitionalCraftingUI.itemListCraftable.navRemoCon.posX + 12

    fun getIconPosY(index: Int) = (panelToggleBarY + ((index*2+1)/4f) * panelToggleBarHeight).roundToInt() - catIconHeight/2

    private val catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    val menuButtonTechView = UIItemImageButton(
        this, catIcons.get(20, 1),
        initialX = panelToggleBarX,
        initialY = getIconPosY(0),
        highlightable = true
    ).also {
        it.clickOnceListener = { _, _ ->
            showTechViewUI()
        }
    }

    val menuButtonCraft = UIItemImageButton(
        this, catIcons.get(19, 1),
        initialX = panelToggleBarX,
        initialY = getIconPosY(1),
        highlightable = true
    ).also {
        it.clickOnceListener = { _, _ ->
            showCraftingUI()
        }
    }

    fun resetUI() {
        transitionalCraftingUI.resetUI()
        transitionalTechtreePanel.resetUI()
    }


    fun showTechViewUI() {
        transitionPanel.setLeftUIto(1)
        transitionalTechtreePanel.show()
        menuButtonTechView.highlighted = true
        menuButtonCraft.highlighted = false
    }

    fun showCraftingUI() {
        transitionPanel.setLeftUIto(0)
        transitionalCraftingUI.show()
        menuButtonTechView.highlighted = false
        menuButtonCraft.highlighted = true
    }

    fun showCraftingUIwithoutAnim() {
        transitionPanel.setLeftUIto(0)
        transitionalCraftingUI.show()
        menuButtonTechView.highlighted = false
        menuButtonCraft.highlighted = true
    }

    private val specialUIitems = listOf(
        menuButtonCraft, menuButtonTechView
    )

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }

        // copy transition of parent (UIItemHorizontalFadeSlide) to this (also UIItemHorizontalFadeSlide)
        full?.let { full ->
            uis.forEach {
                it.posX = full.transitionPanel.getOffX(0)
                it.opacity = full.transitionPanel.getOpacity(0)
            }
            specialUIitems.forEach {
                it.posX = it.initialX + full.transitionPanel.getOffX(0)
            }
        }
    }

    private val cellHighlightNormalCol2 = UIItemInventoryCellCommonRes.defaultInventoryCellTheme.cellHighlightNormalCol.cpy().also { it.a /= 2f }

    private val shader = App.loadShaderInline(UIHandler.SHADER_PROG_VERT, UIHandler.SHADER_PROG_FRAG)

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.shader = null

        if (full == null) {
            UIInventoryFull.drawBackground(batch, opacity)
        }

        // draw fake navbar //
        // draw the tray
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, panelToggleBarX - 4, panelToggleBarY, panelToggleBarWidth, panelToggleBarHeight)
        // cell border
        batch.color = UIItemInventoryCellCommonRes.defaultInventoryCellTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, panelToggleBarX - 4, panelToggleBarY, panelToggleBarWidth, panelToggleBarHeight)
        // cell divider
        batch.color = cellHighlightNormalCol2
        Toolkit.drawStraightLine(batch, panelToggleBarX - 2, panelToggleBarY + panelToggleBarHeight/2, panelToggleBarX-2 + panelToggleBarWidth-4, 1, false)


        transitionPanel.render(frameDelta, batch, camera)

        // make transparency work for button
        batch.shader = shader
        shader.setUniformf("opacity", opacity)
        specialUIitems.forEach {
            it.render(frameDelta, batch, camera)
        }
    }

    override fun dispose() {
        transitionPanel.dispose()
    }

    init {
        addUIitem(transitionPanel)
        addUIitem(menuButtonTechView)
        addUIitem(menuButtonCraft)
    }

    /*override fun setPosition(x: Int, y: Int) {
        transitionalCraftingUI.setPosition(x, y)
        transitionalTechtreePanel.setPosition(x, y)
    }*/

    override fun show() {
        super.show()
        showCraftingUIwithoutAnim() // default to the crafting UI. ALso highlights the right button initially
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
    }
}