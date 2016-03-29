package com.torvald.terrarum.ui

import com.torvald.terrarum.gamemap.PairedMapLayer
import com.torvald.terrarum.langpack.Lang
import com.torvald.terrarum.mapdrawer.LightmapRenderer
import com.torvald.terrarum.mapdrawer.MapCamera
import com.torvald.terrarum.mapdrawer.MapDrawer
import com.torvald.terrarum.Terrarum
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
        val hitbox = player.hitbox!!

        xdelta = hitbox.pointedX - prevPlayerX
        ydelta = hitbox.pointedY - prevPlayerY

        prevPlayerX = hitbox.pointedX
        prevPlayerY = hitbox.pointedY
    }

    override fun render(gc: GameContainer, g: Graphics) {
        val player = Terrarum.game.player

        val sb = StringBuilder()
        val formatter = Formatter(sb)

        val mouseTileX = ((MapCamera.cameraX + gc.getInput().mouseX / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.cameraY + gc.getInput().mouseY / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()

        g.color = Color.white

        val hitbox = player.hitbox
        val nextHitbox = player.nextHitbox

        printLine(g, 1, "posX: "
                + "${hitbox!!.pointedX.toString()}"
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
        val rawR = LightmapRenderer.getRawR(valRaw)
        val rawG = LightmapRenderer.getRawG(valRaw)
        val rawB = LightmapRenderer.getRawB(valRaw)
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
    }

    private fun printLine(g: Graphics, l: Int, s: String) {
        g.drawString(s, 20f, line(l).toFloat())
    }

    private fun printLineColumn(g: Graphics, col: Int, row: Int, s: String) {
        g.drawString(s, (20 + column(col)).toFloat(), line(row).toFloat())
    }

    private fun line(i: Int): Int {
        return i * 20
    }

    private fun column(i: Int): Int {
        return 250 * (i - 1)
    }

    override fun doOpening(gc: GameContainer, delta: Int) {

    }

    override fun doClosing(gc: GameContainer, delta: Int) {

    }

    override fun endOpening(gc: GameContainer, delta: Int) {

    }

    override fun endClosing(gc: GameContainer, delta: Int) {

    }
}