package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
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
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start",
) {
    private val yearCellWidth = 200
    private val cellWidth = 80
    private val cellHeight = 24

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val y = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() + 1 - 34

    private val drawStartX = (Toolkit.drawWidth - cellWidth * 8) / 2 - 4
    private val cellsStartY = y + 34

    private val SP = "\u3000 "
    val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"


    private var todayCell = -1

    private val cellBackCols = listOf(
        Color(0x3f1e22_C8), // OKLCh  14, 5, 18
        Color(0x022f3a_C8), // OKLCh 218, 5, 18
        Color(0x2d2b09_C8), // OKLCh 105, 5, 18
        Color(0x252934_C8)  // OKLCh 265, 2, 18
    )
    private val seasonMarkers = listOf(
        7 to "CONTEXT_CALENDAR_SEASON_SPRING",
        39 to "CONTEXT_CALENDAR_SEASON_SUMMER",
        71 to "CONTEXT_CALENDAR_SEASON_AUTUMN",
        103 to "CONTEXT_CALENDAR_SEASON_WINTER"
    )

    private var mouseOverCell = -1
    private var mouseOverSeason = -1

    override fun updateUI(delta: Float) {
        mouseOverCell = if (relativeMouseX in drawStartX until drawStartX + 8 * (cellWidth + 1) &&
            relativeMouseY in cellsStartY - 1 until cellsStartY - 1 + 17 * (cellHeight + 3)) {

            val x = (relativeMouseX - drawStartX) / (cellWidth + 1)
            val y = (relativeMouseY - cellsStartY + 1) / (cellHeight + 3)

            // disable highlighting on invalid date (verddag and not winter 30)
            if (x == 7 && y < 16) -1
            else y * 8 + x
        }
        else -1

        mouseOverSeason = when (mouseOverCell) {
            -1 -> -1
            in 0 until 34 -> 0
            in 34 until 68 -> 1
            in 68 until 102 -> 2
            else -> 3
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, 1f)

        val thisYear = INGAME.world.worldTime.years
        val today = INGAME.world.worldTime.ordinalDay + 1
        val todayOfWeek = INGAME.world.worldTime.dayOfWeek

        // cell background
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, (width - yearCellWidth) / 2, y - 34, yearCellWidth, 24)
        for (week in 0..7) {
            Toolkit.fillArea(batch, drawStartX + (cellWidth + 1) * week + 1, y, cellWidth - 2, 24)
        }
        for (cellNum in 0 until 17 * 8) {
            batch.color = when (cellNum) {
                in 0 until 34 -> cellBackCols[0]
                in 34 until 68 -> cellBackCols[1]
                in 68 until 102 -> cellBackCols[2]
                else -> cellBackCols[3]
            }
            if (cellNum % 8 != 7 || cellNum == 17 * 8 - 1) {
                Toolkit.fillArea(batch, drawStartX + (cellWidth + 1) * (cellNum % 8) + 1, cellsStartY + (cellHeight + 3) * (cellNum / 8), cellWidth - 2, cellHeight)
            }
        }
        // season name cell background
        for (k in 0..3) {
            batch.color = cellBackCols[k]
            Toolkit.fillArea(batch, drawStartX + (cellWidth + 1) * 7 + 1, cellsStartY + (cellHeight + 3) * (k * 4), cellWidth - 2, (cellHeight + 3) * 4 - 3)
        }


        // cell border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, (width - yearCellWidth) / 2 - 1, y - 35, yearCellWidth + 2, 26)
        Toolkit.drawBoxBorder(batch, drawStartX, y - 1, 8 * (cellWidth + 1) - 1, 26)
        for (week in 0..7) {
            Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * week, y - 1, cellWidth, 26)
        }
        // highlight a day name of mouse-up
        batch.color = Toolkit.Theme.COL_MOUSE_UP
        if (mouseOverCell >= 0) Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * (mouseOverCell % 8), y - 1, cellWidth, 26)
        // highlight today's week name
        batch.color = Toolkit.Theme.COL_SELECTED
        Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * todayOfWeek, y - 1, cellWidth, 26)


        // draw days grid
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, drawStartX, cellsStartY - 1, 8 * (cellWidth + 1) - 1, 17 * (cellHeight + 3) - 1)
        // non-season-name-cells
        for (cellNum in 0 until 17 * 8) {
            if (cellNum % 8 != 7 || cellNum == 17 * 8 - 1) {
                Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * (cellNum % 8), cellsStartY + (cellHeight + 3) * (cellNum / 8) - 1, cellWidth, cellHeight + 2)
            }
        }
        // season-name-cells
        for (k in 0..3) {
            Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * 7, cellsStartY + (cellHeight + 3) * (k * 4) - 1, cellWidth, (cellHeight + 3) * 4 - 1)
        }
        // highlight a day of mouse-up
        batch.color = Toolkit.Theme.COL_MOUSE_UP
        if (mouseOverCell >= 0) Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * (mouseOverCell % 8), cellsStartY + (cellHeight + 3) * (mouseOverCell / 8) - 1, cellWidth, cellHeight + 2)


        // season border
        batch.color = Toolkit.Theme.COL_MOUSE_UP
        if (mouseOverSeason == 0) {
            Toolkit.drawStraightLine(batch,
                drawStartX,
                cellsStartY - 2,
                drawStartX + (cellWidth + 1) * 8 - 1,
                1,
                false
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX - 1,
                cellsStartY - 1,
                cellsStartY + 1 + (cellHeight + 3) * 5 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 8 - 1,
                cellsStartY - 1,
                cellsStartY + 1 + (cellHeight + 3) * 4 - 3,
                1,
                true
            )
        }
        if (mouseOverSeason in 0..1) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX,
                cellsStartY + 1 + (cellHeight + 3) * 5 - 3,
                drawStartX + (cellWidth + 1) * 2 - 1,
                1,
                false
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 2 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 4 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 5 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 2,
                cellsStartY + 1 + (cellHeight + 3) * 4 - 3,
                drawStartX + (cellWidth + 1) * 8 - 1,
                1,
                false
            )
        }
        if (mouseOverSeason == 1) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX - 1,
                cellsStartY + 1 + (cellHeight + 3) * 5 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 9 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 8 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 4 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 8 - 3,
                1,
                true
            )
        }
        if (mouseOverSeason in 1..2) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX,
                cellsStartY + 1 + (cellHeight + 3) * 9 - 3,
                drawStartX + (cellWidth + 1) * 4 - 1,
                1,
                false
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 4 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 8 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 9 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 4,
                cellsStartY + 1 + (cellHeight + 3) * 8 - 3,
                drawStartX + (cellWidth + 1) * 8 - 1,
                1,
                false
            )
        }
        if (mouseOverSeason == 2) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX - 1,
                cellsStartY + 1 + (cellHeight + 3) * 9 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 13 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 8 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 8 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 12 - 3,
                1,
                true
            )
        }
        if (mouseOverSeason in 2..3) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX,
                cellsStartY + 1 + (cellHeight + 3) * 13 - 3,
                drawStartX + (cellWidth + 1) * 6 - 1,
                1,
                false
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 6 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 12 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 13 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 6,
                cellsStartY + 1 + (cellHeight + 3) * 12 - 3,
                drawStartX + (cellWidth + 1) * 8 - 1,
                1,
                false
            )
        }
        if (mouseOverSeason == 3) {
            Toolkit.drawStraightLine(
                batch,
                drawStartX - 1,
                cellsStartY + 1 + (cellHeight + 3) * 13 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 17 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(
                batch,
                drawStartX + (cellWidth + 1) * 8 - 1,
                cellsStartY + 1 + (cellHeight + 3) * 12 - 2,
                cellsStartY + 1 + (cellHeight + 3) * 17 - 3,
                1,
                true
            )
            Toolkit.drawStraightLine(batch,
                drawStartX,
                cellsStartY + 1 + (cellHeight + 3) * 17 - 3,
                drawStartX + (cellWidth + 1) * 8 - 1,
                1,
                false
            )
        }

        // cell texts
        batch.color = Toolkit.Theme.COL_LIST_DEFAULT
        Toolkit.drawTextCentered(batch, App.fontGame, Lang.getAndUseTemplate("CONTEXT_CALENDAR_DATE_FORMAT_Y", false, thisYear), yearCellWidth, (width - yearCellWidth) / 2, y - 34)
        for (week in 0..7) {
            // highlight this week and the mouse-up
            batch.color = if (week == todayOfWeek) Toolkit.Theme.COL_SELECTED else if (week == mouseOverCell % 8) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_LIST_DEFAULT

            val t = WorldTime.getDayName(week)
            val tlen = App.fontGame.getWidth(t)
            App.fontGame.draw(batch, t, drawStartX + (cellWidth + 1) * week + (cellWidth - tlen) / 2, y)
        }
        var dayAkku = 1
        for (cellNum in 0 until 17 * 8) {
            val day = if (cellNum == 17*8-1) 120 else if (cellNum % 8 == 7) 0 else dayAkku

            if (day > 0) {
                // highlight today and the mouse-up
                batch.color = if (day == today) Toolkit.Theme.COL_SELECTED else if (cellNum == mouseOverCell) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_LIST_DEFAULT

                val t = "${(day % MONTH_LENGTH).let { if (it == 0) MONTH_LENGTH else it }}".padStart(2, '\u2007')
                App.fontGame.draw(batch, t, drawStartX + (cellWidth + 1) * (cellNum % 8) - 20 + cellWidth - 4, cellsStartY + (cellHeight + 3) * (cellNum / 8))

                if (day == today) todayCell = cellNum

                dayAkku += 1
            }
        }
        // draw seasonal names
        seasonMarkers.forEachIndexed { index, (cellNum, key) ->
            batch.color = if (index == mouseOverSeason) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawTextCentered(batch, App.fontGame, Lang[key], cellWidth, drawStartX + (cellWidth + 1) * (cellNum % 8), cellsStartY + (cellHeight + 3) * (cellNum / 8) + ((cellHeight + 3) * 1.5f).floorToInt())
        }

        // highlight today cell
        if (todayCell >= 0) {
            batch.color = Toolkit.Theme.COL_SELECTED
            Toolkit.drawBoxBorder(batch, drawStartX + (cellWidth + 1) * (todayCell % 8), cellsStartY + (cellHeight + 3) * (todayCell / 8) - 1, cellWidth, cellHeight + 2)
        }

        // control hints
        batch.color = Color.WHITE
        App.fontGame.draw(batch, controlHelp, drawStartX + 2, cellsStartY+ 17 * (cellHeight + 3) + 6)
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