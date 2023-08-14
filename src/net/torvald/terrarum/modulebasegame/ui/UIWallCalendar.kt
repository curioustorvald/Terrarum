package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.RunningEnvironment
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.gameworld.WorldTime.Companion.MONTH_LENGTH
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2023-08-15.
 */
class UIWallCalendar : UICanvas(
    toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
    toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
) {
    private val cellWidth = 96
    private val cellHeight = 24

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val y = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() + 1 - 34

    private val drawStartX = (Toolkit.drawWidth - cellWidth * 8) / 2 - 4
    private val cellsStartY = y + 34

    private val SP = "\u3000 "
    val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"

    override fun updateUI(delta: Float) {

    }


    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        UIInventoryFull.drawBackground(batch, 1f)

        val today = INGAME.world.worldTime.ordinalDay + 1
        val todayOfWeek = INGAME.world.worldTime.dayOfWeek

        // cell background
        batch.color = UIInventoryFull.CELL_COL
        for (week in 0..7) {
            Toolkit.fillArea(batch, drawStartX + (cellWidth + 1) * week + 1, y, cellWidth - 2, 24)
        }
        for (cellNum in 0 until 17 * 8) {
            Toolkit.fillArea(batch, drawStartX + (cellWidth + 1) * (cellNum % 8) + 1, cellsStartY + (cellHeight + 3) * (cellNum / 8), cellWidth - 2, cellHeight)
        }


        // cell border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, drawStartX, y - 1, 8 * (cellWidth + 1) - 1, 26)
        for (week in 0..7) {
            Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * week, y - 1, cellWidth, 26)
        }

        Toolkit.drawBoxBorder(batch, drawStartX, cellsStartY - 1, 8 * (cellWidth + 1) - 1, 17 * (cellHeight + 3) - 1)
        for (cellNum in 0 until 17 * 8) {
            Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * (cellNum % 8), cellsStartY + (cellHeight + 3) * (cellNum / 8) - 1, cellWidth, cellHeight + 2)
        }


        // cell texts
        for (week in 0..7) {
            batch.color = if (week == todayOfWeek) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_LIST_DEFAULT
            val t = WorldTime.getDayName(week)
            val tlen = App.fontGame.getWidth(t)
            App.fontGame.draw(batch, t, drawStartX + (cellWidth + 1) * week + (cellWidth - tlen) / 2, y)
        }
        var dayAkku = 1
        for (cellNum in 0 until 17 * 8) {
            val day = if (cellNum == 17*8-1) 120 else if (cellNum % 8 == 7) 0 else dayAkku

            if (day > 0) {
                batch.color = if (day == today) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_LIST_DEFAULT
                val t = "${(day % MONTH_LENGTH).let { if (it == 0) MONTH_LENGTH else it }}".padStart(2, '\u2007')
                App.fontGame.draw(batch, t, drawStartX + (cellWidth + 1) * (cellNum % 8) + 1 + cellWidth - 23, cellsStartY + (cellHeight + 3) * (cellNum / 8))
                dayAkku += 1
            }
        }

    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun dispose() {
    }
}