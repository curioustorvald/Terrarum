package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ControlPresets
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.RunningEnvironment
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2025-01-19.
 */
class UIRedeemCodeMachine : UICanvas(
    toggleKeyLiteral = null, // no Q key to close
    toggleButtonLiteral = "control_gamepad_start",
) {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    val title = UIItemTextLabel(this, { "Enter the Code" },
        (Toolkit.drawWidth - UIItemRedeemCodeArea.estimateWidth(14)) / 2,
        App.scr.halfh - UIItemRedeemCodeArea.estimateHeight(4) - 48 - 48,
        UIItemRedeemCodeArea.estimateWidth(14)
        )

    val inputPanel = UIItemRedeemCodeArea(this,
        (Toolkit.drawWidth - UIItemRedeemCodeArea.estimateWidth(14)) / 2,
        App.scr.halfh - UIItemRedeemCodeArea.estimateHeight(4) - 48,
        14, 4)

    init {
        addUIitem(title)
        addUIitem(inputPanel)
    }

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(Input.Keys.ESCAPE)} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]} "


    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val yEnd = -UIInventoryFull.YPOS_CORRECTION + (App.scr.height + UIInventoryFull.internalHeight).div(2).toFloat()


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, 1f)
        uiItems.forEach { it.render(frameDelta, batch, camera) }

        val controlHintXPos = thisOffsetX + 2f
        App.fontGame.draw(batch, controlHelp, controlHintXPos, yEnd - 20)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
    }

    override fun dispose() {
    }

}