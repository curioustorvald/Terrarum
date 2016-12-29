package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendScreen
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-03-14.
 */
class BasicDebugInfoWindow : UICanvas {

    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    override var openCloseTime: Int = 0

    override var handler: UIHandler? = null

    private var prevPlayerX = 0.0
    private var prevPlayerY = 0.0

    private var xdelta = 0.0
    private var ydelta = 0.0

    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccR = GameFontBase.colToCode["r"]
    val ccM = GameFontBase.colToCode["m"]

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {

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

        val mouseTileX = ((MapCamera.cameraX + gc.input.mouseX / Terrarum.ingame.screenZoom) / MapDrawer.TILE_SIZE).toInt()
        val mouseTileY = ((MapCamera.cameraY + gc.input.mouseY / Terrarum.ingame.screenZoom) / MapDrawer.TILE_SIZE).toInt()

        g.font = Terrarum.fontSmallNumbers
        g.color = GameFontBase.codeToCol["y"]

        val hitbox = player.hitbox
        val nextHitbox = player.nextHitbox

        /**
         * First column
         */

        printLine(g, 1, "posX "
                + ccG
                + "${hitbox.pointedX}"
                + " ("
                + "${(hitbox.pointedX / MapDrawer.TILE_SIZE).toInt()}"
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

        //printLine(g, 7, "jump $ccG${player.jumpAcc}")

        val lightVal: String
        val mtX = mouseTileX.toString()
        val mtY = mouseTileY.toString()
        val valRaw = LightmapRenderer.getValueFromMap(mouseTileX, mouseTileY) ?: -1
        val rawR = valRaw.rawR()
        val rawG = valRaw.rawG()
        val rawB = valRaw.rawB()

        lightVal = if (valRaw == -1) "—"
                   else valRaw.toString() + " (" +
                    rawR.toString() + " " +
                    rawG.toString() + " " +
                    rawB.toString() + ")"
        printLine(g, 8, "light@cursor $ccG$lightVal")

        val tileNo: String
        val tileNumRaw = Terrarum.ingame.world.getTileFromTerrain(mouseTileX, mouseTileY) ?: -1
        val tilenum = tileNumRaw / PairedMapLayer.RANGE
        val tiledmg = tileNumRaw % PairedMapLayer.RANGE
        tileNo = if (tileNumRaw == -1) "—" else "$tilenum:$tiledmg"

        printLine(g, 9, "tile@cursor $ccG$tileNo ($mtX, $mtY)")

        /**
         * Second column
         */

        printLineColumn(g, 2, 1, "VSync $ccG" + Terrarum.appgc.isVSyncRequested)
        printLineColumn(g, 2, 2, "Env colour temp $ccG" + MapDrawer.colTemp)
        printLineColumn(g, 2, 5, "Time $ccG${Terrarum.ingame.world.time.elapsedSeconds}" +
                                 " (${Terrarum.ingame.world.time.getFormattedTime()})")
        printLineColumn(g, 2, 6, "Mass $ccG${player.mass}")

        printLineColumn(g, 2, 7, "p_WalkX $ccG${player.walkX}")
        printLineColumn(g, 2, 8, "p_WalkY $ccG${player.walkY}")


        drawHistogram(g, LightmapRenderer.histogram,
                Terrarum.WIDTH - histogramW - 30,
                Terrarum.HEIGHT - histogramH - 30
        )
        if (Terrarum.controller != null) {
            drawGamepadAxis(g,
                    Terrarum.controller!!.getAxisValue(3),
                    Terrarum.controller!!.getAxisValue(2),
                    Terrarum.WIDTH - 135,
                    40
            )
        }

        /**
         * Top right
         */

        g.color = GameFontBase.codeToCol["y"]
        g.drawString("${ccY}MEM ", (Terrarum.WIDTH - 15 * 8 - 2).toFloat(), 2f)
        //g.drawString("${ccY}FPS $ccG${Terrarum.appgc.fps}", (Terrarum.WIDTH - 6 * 8 - 2).toFloat(), 10f)
        g.drawString("${ccY}CPUs ${if (Terrarum.MULTITHREAD) ccG else ccR}${Terrarum.THREADS}",
                (Terrarum.WIDTH - 2 - 6*8).toFloat(), 10f)

        g.color = GameFontBase.codeToCol["g"]
        g.drawString("${Terrarum.memInUse}M",
                (Terrarum.WIDTH - 11 * 8 - 2).toFloat(), 2f)
        g.drawString("/${Terrarum.totalVMMem}M",
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
        g.drawString(s, 10f, line(l))
    }

    private fun printLineColumn(g: Graphics, col: Int, row: Int, s: String) {
        g.drawString(s, (10 + column(col)), line(row))
    }

    val histogramW = 256
    val histogramH = 200

    private fun drawHistogram(g: Graphics, histogram: LightmapRenderer.Histogram, x: Int, y: Int) {
        val uiColour = Color(0xAA000000.toInt())
        val barR = Color(0xDD0000)
        val barG = Color(0x00DD00)
        val barB = Color(0x0000DD)
        val barColour = arrayOf(barR, barG, barB)
        val w = histogramW.toFloat()
        val h = histogramH.toFloat()
        val range = histogram.range
        val histogramMax = histogram.screen_tiles.toFloat()

        g.color = uiColour
        g.fillRect(x.toFloat(), y.toFloat(), w.plus(1), h)
        g.color = Color.gray
        g.drawString("0", x.toFloat(), y.toFloat() + h + 2)
        g.drawString("255", x.toFloat() + w + 1 - 8*3, y.toFloat() + h + 2)
        g.drawString("Histogramme", x + w / 2 - 5.5f * 8, y.toFloat() + h + 2)

        blendScreen()
        for (c in 0..2) {
            for (i in 0..255) {
                var histogram_value = if (i == 255) 0 else histogram.get(c)[i]
                if (i == 255) {
                    for (k in 255..range - 1) {
                        histogram_value += histogram.get(c)[k]
                    }
                }

                val bar_x = x + (w / w.minus(1f)) * i.toFloat()
                val bar_h = FastMath.ceil(h / histogramMax * histogram_value.toFloat()).toFloat()
                val bar_y = y + (h / histogramMax) - bar_h + h
                val bar_w = 1f

                g.color = barColour[c]
                g.fillRect(bar_x, bar_y, bar_w, bar_h)
            }
        }
        blendNormal()
    }

    private fun drawGamepadAxis(g: Graphics, axisX: Float, axisY: Float, uiX: Int, uiY: Int) {
        val uiColour = Color(0xAA000000.toInt())
        val w = 128f
        val h = 128f
        val halfW = w / 2f
        val halfH = h / 2f

        val pointDX = axisX * halfW
        val pointDY = axisY * halfH

        val padName = if (Terrarum.controller!!.name.isEmpty()) "Gamepad"
                      else Terrarum.controller!!.name

        blendNormal()

        g.color = uiColour
        g.fillRect(uiX.toFloat(), uiY.toFloat(), w, h)
        g.color = Color.white
        g.drawLine(uiX + halfW, uiY + halfH, uiX + halfW + pointDX, uiY + halfH + pointDY)
        g.color = Color.gray
        g.drawString(padName, uiX + w / 2 - (padName.length) * 4, uiY.toFloat() + h + 2)
    }

    private fun line(i: Int): Float = i * 10f

    private fun column(i: Int): Float = 300f * (i - 1)

    override fun doOpening(gc: GameContainer, delta: Int) {

    }

    override fun doClosing(gc: GameContainer, delta: Int) {

    }

    override fun endOpening(gc: GameContainer, delta: Int) {

    }

    override fun endClosing(gc: GameContainer, delta: Int) {

    }
}