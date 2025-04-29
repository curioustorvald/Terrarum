package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ControlPresets
import net.torvald.terrarum.RunningEnvironment
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.unicode.getKeycapPC

/**
 * UI for ItemClipboard
 *
 * Created by minjaesong on 2025-04-13.
 */
class UIClipboardItem : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start",
) {
    override var width: Int = 800
    override var height: Int = App.scr.height

    private val y = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() + 1 - 34

    private val drawStartX = (Toolkit.drawWidth - width) / 2 - 4
    private val cellsStartY = y + 34

    private val SP = "\u3000 "
    val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"


    override fun updateImpl(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}