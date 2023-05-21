package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemHorizontalFadeSlide

/**
 * Structure:
 *
 * UIWorldPortal (the container)
 * + UIWorldPortalSearch (left panel)
 * + UIWorldPortalListing (centre panel)
 * + UIWorldPortalCargo (right panel)
 *
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortal : UICanvas() {

    override var width = App.scr.width
    override var height = App.scr.height



    val gradStartCol = Color(0x404040_60)
    val gradEndCol   = Color(0x000000_70)
    val gradHeight = 48f


    val controlHelpHeight = App.fontGame.lineHeight

    private var panelTransitionLocked = false

    fun lockTransition() {
        panelTransitionLocked = true
    }
    fun unlockTransition() {
        panelTransitionLocked = false
    }
    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)


    val catBar = UIItemInventoryCatBar(
        this,
        (width - UIInventoryFull.catBarWidth) / 2,
        42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
        UIInventoryFull.internalWidth,
        UIInventoryFull.catBarWidth,
        true
    ) { i -> if (!panelTransitionLocked) requestTransition(i) }


    private val transitionalSearch = UIWorldPortalSearch(this)
    private val transitionalListing = UIWorldPortalListing(this)
    private val transitionalCargo = UIWorldPortalCargo(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - UIInventoryFull.internalWidth) / 2,
        UIInventoryFull.INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        1f,
        transitionalSearch, transitionalListing, transitionalCargo
    )

    init {
        addUIitem(catBar)
        addUIitem(transitionPanel)



    }






    override fun updateUI(delta: Float) {

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        UIInventoryFull.drawBackground(batch, handler.opacity)

        // UI items
        catBar.render(batch, camera)
        transitionPanel.render(batch, camera)
    }

    override fun dispose() {
        catBar.dispose()
    }

    fun resetUI() {

    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        resetUI()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        resetUI()
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

}