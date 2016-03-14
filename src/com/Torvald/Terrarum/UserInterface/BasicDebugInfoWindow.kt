package com.Torvald.Terrarum.UserInterface

import com.Torvald.Terrarum.GameMap.PairedMapLayer
import com.Torvald.Terrarum.LangPack.Lang
import com.Torvald.Terrarum.MapDrawer.LightmapRenderer
import com.Torvald.Terrarum.MapDrawer.MapCamera
import com.Torvald.Terrarum.MapDrawer.MapDrawer
import com.Torvald.Terrarum.Terrarum
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
class BasicDebugInfoWindow : UICanvas {

    override var width: Int? = Terrarum.WIDTH
    override var height: Int? = Terrarum.HEIGHT

    override fun processInput(input: Input) {

    }

    override fun update(gc: GameContainer, delta_t: Int) {

    }

    override fun render(gc: GameContainer, g: Graphics) {
        val player = Terrarum.game.player

        val sb = StringBuilder()
        val formatter = Formatter(sb)

        val mouseTileX = ((MapCamera.getCameraX() + gc.getInput().mouseX / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.getCameraY() + gc.getInput().mouseY / Terrarum.game.screenZoom) / MapDrawer.TILE_SIZE).toInt()

        g.setColor(Color.white)

        val hitbox = player.hitbox
        val nextHitbox = player.nextHitbox

        printLine(g, 1, "posX : "
                + "${hitbox!!.pointedX.toString()}"
                + " ("
                + "${(hitbox!!.pointedX / MapDrawer.TILE_SIZE).toInt().toString()}"
                + ")")
        printLine(g, 2, "posY : "
                + hitbox.pointedY.toString()
                + " ("
                + (hitbox.pointedY / MapDrawer.TILE_SIZE).toInt().toString()
                + ")")
        printLine(g, 3, "veloX : ${player.veloX}")
        printLine(g, 4, "veloY : ${player.veloY}")
        printLine(g, 5, "grounded : ${player.grounded}")
        printLine(g, 6, "noClip : ${player.noClip}")
        printLine(g, 7, "mass : ${player.mass} [kg]")

        val lightVal: String
        var mtX = mouseTileX.toString()
        var mtY = mouseTileY.toString()
        try {
            val valRaw = LightmapRenderer.getValueFromMap(mouseTileX, mouseTileY)
            val rawR = LightmapRenderer.getRawR(valRaw)
            val rawG = LightmapRenderer.getRawG(valRaw)
            val rawB = LightmapRenderer.getRawB(valRaw)
            lightVal = valRaw.toInt().toString() + " (" +
                    rawR.toString() + " " +
                    rawG.toString() + " " +
                    rawB.toString() + ")"
        } catch (e: ArrayIndexOutOfBoundsException) {
            lightVal = "out of bounds"
            mtX = "---"
            mtY = "---"
        }

        printLine(g, 8, "light at cursor : " + lightVal)

        val tileNo: String
        try {
            val tileNumRaw = Terrarum.game.map.getTileFromTerrain(mouseTileX, mouseTileY)
            val tilenum = tileNumRaw / PairedMapLayer.RANGE
            val tiledmg = tileNumRaw % PairedMapLayer.RANGE
            tileNo = "$tilenum:$tiledmg"
        } catch (e: ArrayIndexOutOfBoundsException) {
            tileNo = "-"
        }

        printLine(g, 9, "tile : $tileNo ($mtX, $mtY)")

        /**
         * Second column
         */

        printLineColumn(g, 2, 1, "Vsync : " + Terrarum.appgc.isVSyncRequested)
        printLineColumn(g, 2, 2, "Env colour temp : " + MapDrawer.getColTemp())

        /**
         * On screen
         */

        // Memory allocation
        val memInUse = Terrarum.game.memInUse
        val totalVMMem = Terrarum.game.totalVMMem

        g.setColor(Color(0xFF7F00))
        g.drawString(
                Lang.get("DEV_MEMORY_SHORT_CAP")
                        + " : "
                        + formatter.format(
                        Lang.get("DEV_MEMORY_A_OF_B"), memInUse, totalVMMem), (Terrarum.WIDTH - 200).toFloat(), line(1).toFloat())

        // Hitbox
        val zoom = Terrarum.game.screenZoom
        g.setColor(Color(0x007f00))
        g.drawRect(hitbox.getHitboxStart().getX() * zoom - MapCamera.getCameraX() * zoom, hitbox.getHitboxStart().getY() * zoom - MapCamera.getCameraY() * zoom, hitbox.getWidth() * zoom, hitbox.getHeight() * zoom)
        // ...and its point
        g.fillRect(
                (hitbox.getPointedX() - 1) * zoom - MapCamera.getCameraX() * zoom, (hitbox.getPointedY() - 1) * zoom - MapCamera.getCameraY() * zoom, 3f, 3f)
        g.drawString(
                Lang.get("DEV_COLOUR_LEGEND_GREEN") + " :  hitbox", (Terrarum.WIDTH - 200).toFloat(), line(2).toFloat())

        // Next hitbox
        g.setColor(Color.blue)
        g.drawRect(nextHitbox!!.getHitboxStart().getX() * zoom - MapCamera.getCameraX() * zoom, nextHitbox.getHitboxStart().getY() * zoom - MapCamera.getCameraY() * zoom, nextHitbox.getWidth() * zoom, nextHitbox.getHeight() * zoom)
        // ...and its point
        g.fillRect(
                (nextHitbox!!.getPointedX() - 1) * zoom - MapCamera.getCameraX() * zoom, (nextHitbox.getPointedY() - 1) * zoom - MapCamera.getCameraY() * zoom, 3f, 3f)
        g.drawString(
                Lang.get("DEV_COLOUR_LEGEND_BLUE") + " :  nextHitbox", (Terrarum.WIDTH - 200).toFloat(), line(3).toFloat())
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
}