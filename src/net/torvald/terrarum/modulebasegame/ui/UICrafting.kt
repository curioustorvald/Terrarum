package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemHorizontalFadeSlide

/**
 * Created by minjaesong on 2024-03-02.
 */
class UICrafting(val full: UIInventoryFull?) : UICanvas(
    toggleKeyLiteral = if (full == null) "control_key_inventory" else null,
    toggleButtonLiteral = if (full == null) "control_gamepad_start" else null
) {

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

    fun resetUI() {
        transitionalCraftingUI.resetUI()
        transitionalTechtreePanel.resetUI()
    }

    fun showCraftingUI() {
        transitionPanel.setLeftUIto(0)
        transitionalCraftingUI.show()
    }

    fun showTechViewUI() {
        transitionPanel.setLeftUIto(1)
        transitionalTechtreePanel.show()
    }

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }

        // copy transition of parent (UIItemHorizontalFadeSlide) to this (also UIItemHorizontalFadeSlide)
        full?.let { full ->
            uis.forEach {
                it.posX = full.transitionPanel.getOffX(0)
                it.opacity = full.transitionPanel.getOpacity(0)
            }
        }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        if (full == null) {
            UIInventoryFull.drawBackground(batch, opacity)
        }

        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
        transitionPanel.dispose()
    }

    init {
        addUIitem(transitionPanel)
    }

    override fun setPosition(x: Int, y: Int) {
        transitionalCraftingUI.setPosition(x, y)
        transitionalTechtreePanel.setPosition(x, y)
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