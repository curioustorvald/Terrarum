package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.imagefont.GameFontBase
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
class BasicDebugInfoWindow:UICanvas {

    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    override var openCloseTime: Int = 0

    private var prevPlayerX = 0.0
    private var prevPlayerY = 0.0

    private var xdelta = 0.0
    private var ydelta = 0.0

    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccR = GameFontBase.colToCode["r"]
    val ccM = GameFontBase.colToCode["m"]

    override fun processInput(input: Input) {

    }

    override fun update(gc: GameContainer, delta: Int) {
        val player = Terrarum.ingame.player
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

        val player = Terrarum.ingame.player

        val sb = StringBuilder()
        val formatter = Formatter(sb)

        val mouseTileX = ((MapCamera.cameraX + gc.input.mouseX / Terrarum.ingame.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.cameraY + gc.input.mouseY / Terrarum.ingame.screenZoom) / MapDrawer.TILE_SIZE).toInt()

        g.font = Terrarum.smallNumbers
        g.color = GameFontBase.codeToCol["y"]

        val hitbox = player.hitbox
        val nextHitbox = player.nextHitbox

        /**
         * First column
         */

        printLine(g, 1, "posX "
                + ccG
                + "${hitbox.pointedX.toString()}"
                + " ("
                + "${(hitbox.pointedX / MapDrawer.TILE_SIZE).toInt().toString()}"
                + ")")
        printLine(g, 2, "posY "
                + ccG
                + hitbox.pointedY.toString()
                + " ("
                + (hitbox.pointedY / MapDrawer.TILE_SIZE).toInt().toString()
                + ")")

        printLine(g, 3, "veloX reported $ccG${player.moveDelta.x}")
        printLine(g, 4, "veloY reported $ccG${player.moveDelta.y}")

        printLineColumn(g, 2, 3, "veloX measured $ccG${xdelta}")
        printLineColumn(g, 2, 4, "veloY measured $ccG${ydelta}")

        printLine(g, 5, "grounded $ccG${player.grounded}")
        printLine(g, 6, "noClip $ccG${player.noClip}")

        val lightVal: String
        val mtX = mouseTileX.toString()
        val mtY = mouseTileY.toString()
        val valRaw = LightmapRenderer.getValueFromMap(mouseTileX, mouseTileY) ?: -1
        val rawR = valRaw.rawR()
        val rawG = valRaw.rawG()
        val rawB = valRaw.rawB()

        lightVal = if (valRaw == -1) "—"
                   else valRaw.toInt().toString() + " (" +
                    rawR.toString() + " " +
                    rawG.toString() + " " +
                    rawB.toString() + ")"
        printLine(g, 7, "light@cursor $ccG$lightVal")

        val tileNo: String
        val tileNumRaw = Terrarum.ingame.map.getTileFromTerrain(mouseTileX, mouseTileY) ?: -1
        val tilenum = tileNumRaw / PairedMapLayer.RANGE
        val tiledmg = tileNumRaw % PairedMapLayer.RANGE
        tileNo = if (tileNumRaw == -1) "—" else "$tilenum:$tiledmg"

        printLine(g, 8, "tile@cursor $ccG$tileNo ($mtX, $mtY)")

        /**
         * Second column
         */

        printLineColumn(g, 2, 1, "VSync $ccG" + Terrarum.appgc.isVSyncRequested)
        printLineColumn(g, 2, 2, "Env colour temp $ccG" + MapDrawer.getColTemp())
        printLineColumn(g, 2, 5, "Time $ccG${Terrarum.ingame.map.worldTime.elapsedSeconds()}" +
                                 " (${Terrarum.ingame.map.worldTime.getFormattedTime()})")
        printLineColumn(g, 2, 6, "Mass $ccG${player.mass}")

        drawHistogram(g, LightmapRenderer.histogram,
                Terrarum.WIDTH - histogramW - 30,
                Terrarum.HEIGHT - histogramH - 30
        )

        /**
         * Top right
         */

        g.color = GameFontBase.codeToCol["y"]
        g.drawString("${ccY}MEM ", (Terrarum.WIDTH - 15 * 8 - 2).toFloat(), 2f)
        //g.drawString("${ccY}FPS $ccG${Terrarum.appgc.fps}", (Terrarum.WIDTH - 6 * 8 - 2).toFloat(), 10f)
        g.drawString("${ccY}CPUs ${if (Terrarum.MULTITHREAD) ccG else ccR}${Terrarum.CORES}",
                (Terrarum.WIDTH - 2 - 6*8).toFloat(), 10f)

        g.color = GameFontBase.codeToCol["g"]
        g.drawString("${Terrarum.ingame.memInUse}M",
                (Terrarum.WIDTH - 11 * 8 - 2).toFloat(), 2f)
        g.drawString("/${Terrarum.ingame.totalVMMem}M",
                (Terrarum.WIDTH - 6 * 8 - 2).toFloat(), 2f)

        /**
         * Bottom left
         */

        g.drawString("${ccY}Actors total $ccG${Terrarum.ingame.actorContainer.size + Terrarum.ingame.actorContainerInactive.size}",
                2f, Terrarum.HEIGHT - 10f)
        g.drawString("${ccY}Active $ccG${Terrarum.ingame.actorContainer.size}",
                (2 + 17*8).toFloat(), Terrarum.HEIGHT - 10f)
        g.drawString("${ccY}Dormant $ccG${Terrarum.ingame.actorContainerInactive.size}",
                (2 + 28*8).toFloat(), Terrarum.HEIGHT - 10f)
    }

    private fun printLine(g: Graphics, l: Int, s: String) {
        g.drawString(s, 10f, line(l).toFloat())
    }

    private fun printLineColumn(g: Graphics, col: Int, row: Int, s: String) {
        g.drawString(s, (10 + column(col)).toFloat(), line(row).toFloat())
    }

    val histogramW = 256
    val histogramH = 200

    private fun drawHistogram(g: Graphics, histogram: LightmapRenderer.Histogram, x: Int, y: Int) {
        val uiColour = Color(0xAA000000.toInt())
        val barR = Color(0xDD0000.toInt())
        val barG = Color(0x00DD00.toInt())
        val barB = Color(0x0000DD.toInt())
        val barColour = arrayOf(barR, barG, barB)
        val w = histogramW.toFloat()
        val h = histogramH.toFloat()
        val range = histogram.range
        val histogramMax = histogram.screen_tiles.toFloat()

        g.color = uiColour
        g.fillRect(x.toFloat(), y.toFloat(), w.plus(1).toFloat(), h.toFloat())
        g.color = Color.gray
        g.drawString("0", x.toFloat(), y.toFloat() + h + 2)
        g.drawString("255", x.toFloat() + w + 1 - 8*3, y.toFloat() + h + 2)
        g.drawString("Histogramme", x + w / 2 - 5.5f * 8, y.toFloat() + h + 2)

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
                val bar_h = FastMath.ceil(h / histogramMax * histogram_value.toFloat()).toFloat()
                val bar_y = y + (h / histogramMax) - bar_h + h
                val bar_w = 1f

                g.color = barColour[c]
                g.fillRect(bar_x, bar_y, bar_w, bar_h)
            }
        }
        setBlendNormal()
    }

    private fun line(i: Int): Float = i * 10f

    private fun column(i: Int): Float = 250f * (i - 1)

    override fun doOpening(gc: GameContainer, delta: Int) {

    }

    override fun doClosing(gc: GameContainer, delta: Int) {

    }

    override fun endOpening(gc: GameContainer, delta: Int) {

    }

    override fun endClosing(gc: GameContainer, delta: Int) {

    }
}