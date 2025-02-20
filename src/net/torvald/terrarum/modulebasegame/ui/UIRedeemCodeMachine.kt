package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ControlPresets
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.RunningEnvironment
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
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

    private val codeCols = 12

    val title = UIItemTextLabel(this, { "Enter the Code" },
        (Toolkit.drawWidth - UIItemRedeemCodeArea.estimateWidth(codeCols)) / 2,
        App.scr.halfh - UIItemRedeemCodeArea.estimateHeight(4) - 48 - 48,
        UIItemRedeemCodeArea.estimateWidth(codeCols)
        )

    val inputPanel = UIItemRedeemCodeArea(this,
        (Toolkit.drawWidth - UIItemRedeemCodeArea.estimateWidth(codeCols)) / 2,
        App.scr.halfh - UIItemRedeemCodeArea.estimateHeight(4) - 48,
        codeCols, 4)

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

    private val alphnums = (('0'..'9') + ('a'..'z') + ('A'..'Z') + '@').map { "$it" }.toHashSet()

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        super.inputStrobed(e)

        if (alphnums.contains(e.character)) {
            inputPanel.acceptChar(e.character[0].uppercaseChar())
        }
        else if (e.keycodes[0] == Input.Keys.BACKSPACE && e.keycodes[1] == 0) {
            inputPanel.backspace()
        }
        else if (e.keycodes[0] == Input.Keys.FORWARD_DEL && e.keycodes[1] == 0) {
            inputPanel.reverseBackspace()
        }
        else if (e.keycodes[0] == Input.Keys.LEFT && e.keycodes[1] == 0) {
            inputPanel.__moveCursorBackward(1)
        }
        else if (e.keycodes[0] == Input.Keys.RIGHT && e.keycodes[1] == 0) {
            inputPanel.__moveCursorForward(1)
        }
    }

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