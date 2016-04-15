package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.terrarum.gamemap.PairedMapLayer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.setBlendNormal
import net.torvald.terrarum.setBlendScreen
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
class BasicDebugInfoWindow : UICanvas {

    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    override var openCloseTime: Int = 0

    private var prevPlayerX = 0f
    private var prevPlayerY = 0f

    private var xdelta = 0f
    private var ydelta = 0f

    override fun processInput(input: Input) {

    }

    override fun update(gc: GameContainer, delta: Int) {
        val player = Terrarum.game.player
        val hitbox = player.hitbox

        xdelta = hitbox.pointedX - prevPlayerX
        ydelta = hitbox.pointedY - prevPlayerY

        prevPlayerX = hitbox.pointedX
        prevPlayerY = hitbox.pointedY
    }

    override fun render(gc: GameContainer, g: Graphics) {
        fun Int.rawR() = this / LightmapRenderer.MUL_2
        fun Int.rawG() = this % LightmapRenderer.MUL_2 / LightmapRenderer.MUL
        fun Int.rawB() = this % LightmapRenderer.MUL

        val player = Terrarum.game.player

        val sb = StringBuilder()
        val formatter = Formatter(sb)

        val mouseTileX = ((MapCamera.cameraX + gc.input.mouseX / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.cameraY + gc.input.mouseY / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()

        g.color = Color.white

        val hitbox = player.hitbox
        val nextHitbox = player.nextHitbox

        printLine(g, 1, "posX: "
                + "${hitbox.pointedX.toString()}"
                + " ("
                + "${(hitbox.pointedX / MapDrawer.TILE_SIZE).toInt().toString()}"
                + ")")
        printLine(g, 2, "posY: "
                + hitbox.pointedY.toString()
                + " ("
                + (hitbox.pointedY / MapDrawer.TILE_SIZE).toInt().toString()
                + ")")

        printLine(g, 3, "veloX reported: ${player.veloX}")
        printLine(g, 4, "veloY reported: ${player.veloY}")

        printLineColumn(g, 2, 3, "veloX measured: ${xdelta}")
        printLineColumn(g, 2, 4, "veloY measured: ${ydelta}")

        printLine(g, 5, "grounded : ${player.grounded}")
        printLine(g, 6, "noClip : ${player.noClip}")

        val lightVal: String
        var mtX = mouseTileX.toString()
        var mtY = mouseTileY.toString()
        val valRaw = LightmapRenderer.getValueFromMap(mouseTileX, mouseTileY) ?: -1
        val rawR = valRaw.rawR()
        val rawG = valRaw.rawG()
        val rawB = valRaw.rawB()
        lightVal = if (valRaw == -1)
            "—"
        else
            valRaw.toInt().toString() + " (" +
                    rawR.toString() + " " +
                    rawG.toString() + " " +
                    rawB.toString() + ")"


        printLine(g, 7, "light at cursor : " + lightVal)

        val tileNo: String
        val tileNumRaw = Terrarum.game.map.getTileFromTerrain(mouseTileX, mouseTileY) ?: -1
        val tilenum = tileNumRaw / PairedMapLayer.RANGE
        val tiledmg = tileNumRaw % PairedMapLayer.RANGE
        tileNo = if (tileNumRaw == -1) "—" else "$tilenum:$tiledmg"

        printLine(g, 8, "tile at cursor : $tileNo ($mtX, $mtY)")

        /**
         * Second column
         */

        printLineColumn(g, 2, 1, "${Lang["MENU_OPTIONS_VSYNC"]} : " + Terrarum.appgc.isVSyncRequested)
        printLineColumn(g, 2, 2, "Env colour temp : " + MapDrawer.getColTemp())
        printLineColumn(g, 2, 5, "Time : ${Terrarum.game.map.worldTime.elapsedSeconds()}" +
                                 " (${Terrarum.game.map.worldTime.getFormattedTime()})")
        printLineColumn(g, 2, 6, "Mass : ${player.mass}")

        /**
         * On screen
         */

        // Memory allocation
        val memInUse = Terrarum.game.memInUse
        val totalVMMem = Terrarum.game.totalVMMem

        g.color = Color(0xFF7F00)
        g.drawString(
                Lang["DEV_MEMORY_SHORT_CAP"]
                        + " : "
                        + formatter.format(
                        Lang["DEV_MEMORY_A_OF_B"], memInUse, totalVMMem), (Terrarum.WIDTH - 200).toFloat(), line(1).toFloat())

        // Hitbox
        val zoom = Terrarum.game.screenZoom
        g.color = Color(0x007f00)
        g.drawRect(hitbox.hitboxStart.x * zoom - MapCamera.cameraX * zoom
                , hitbox.hitboxStart.y * zoom - MapCamera.cameraY * zoom
                , hitbox.width * zoom
                , hitbox.height * zoom)
        // ...and its point
        g.fillRect(
                (hitbox.pointedX - 1) * zoom - MapCamera.cameraX * zoom
                , (hitbox.pointedY - 1) * zoom - MapCamera.cameraY * zoom
                , 3f, 3f)
        g.drawString(
                Lang["DEV_COLOUR_LEGEND_GREEN"] + " :  hitbox", (Terrarum.WIDTH - 200).toFloat()
                , line(2).toFloat())

        // Next hitbox
        g.color = Color.blue
        g.drawRect(nextHitbox!!.hitboxStart.x * zoom - MapCamera.cameraX * zoom
                , nextHitbox.hitboxStart.y * zoom - MapCamera.cameraY * zoom
                , nextHitbox.width * zoom
                , nextHitbox.height * zoom)
        // ...and its point
        g.fillRect(
                (nextHitbox.pointedX - 1) * zoom - MapCamera.cameraX * zoom
                , (nextHitbox.pointedY - 1) * zoom - MapCamera.cameraY * zoom
                , 3f, 3f)
        g.drawString(
                Lang["DEV_COLOUR_LEGEND_BLUE"] + " :  nextHitbox", (Terrarum.WIDTH - 200).toFloat()
                , line(3).toFloat())

        drawHistogram(g, LightmapRenderer.histogram, Terrarum.WIDTH - 256 - 30, Terrarum.HEIGHT - 100 - 30)
    }

    private fun printLine(g: Graphics, l: Int, s: String) {
        g.drawString(s, 20f, line(l).toFloat())
    }

    private fun printLineColumn(g: Graphics, col: Int, row: Int, s: String) {
        g.drawString(s, (20 + column(col)).toFloat(), line(row).toFloat())
    }

    private fun drawHistogram(g: Graphics, histogram: LightmapRenderer.Histogram, x: Int, y: Int) {
        val uiColour = Color(0x99000000.toInt())
        val barR = Color(0xFF0000.toInt())
        val barG = Color(0x00FF00.toInt())
        val barB = Color(0x0000FF.toInt())
        val barColour = arrayOf(barR, barG, barB)
        val w = 256.toFloat()
        val h = 100.toFloat()
        val range = histogram.range
        val histogramMax = histogram.screen_tiles
        //val histogramMax = histogram.histogramMax

        g.color = uiColour
        g.fillRect(x.toFloat(), y.toFloat(), w.plus(1).toFloat(), h.toFloat())

        setBlendScreen()
        for (c in 0..2) {
            for (i in 0..255) {
                var histogram_value = if (i == 255) 0 else histogram.get(c)[i]
                if (i == 255) {
                    for (k in 255..range - 1) {
                        histogram_value += histogram.get(c)[k]
                    }
                }

                val bar_x = x + (w / w.minus(1).toFloat()) * i.toFloat()
                val bar_h = FastMath.ceil(h / histogramMax.toFloat()) * histogram_value.toFloat()
                val bar_y = y + (h / histogramMax.toFloat()) - bar_h + h
                val bar_w = 1f
                g.color = barColour[c]
                g.fillRect(bar_x, bar_y, bar_w, bar_h)
            }
        }
        setBlendNormal()
    }

    private fun line(i: Int): Int = i * 20

    private fun column(i: Int): Int = 250 * (i - 1)

    override fun doOpening(gc: GameContainer, delta: Int) {

    }

    override fun doClosing(gc: GameContainer, delta: Int) {

    }

    override fun endOpening(gc: GameContainer, delta: Int) {

    }

    override fun endClosing(gc: GameContainer, delta: Int) {

    }
}