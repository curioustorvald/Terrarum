package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.unicode.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum.mouseTileX
import net.torvald.terrarum.Terrarum.mouseTileY
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2016-03-14.
 */
class BasicDebugInfoWindow : UICanvas() {

    override var width: Int = App.scr.width
    override var height: Int = App.scr.height

    override var openCloseTime: Float = 0f

    private var prevPlayerX = 0.0
    private var prevPlayerY = 0.0

    private var xdelta = 0.0
    private var ydelta = 0.0

    private val ingame: IngameInstance?
        get() = Terrarum.ingame

    private val world: GameWorld?
        get() = Terrarum.ingame?.world

    private val icons = TextureRegionPack(Gdx.files.internal("assets/graphics/gui/debug_window_symbols.tga"), 21, 26)


    private val ARROW_RIGHT = 0xC0.toChar()
    private val ARROW_LEFT = 0xC1.toChar()
    private val ARROW_UP = 0xCE.toChar()
    private val ARROW_DOWN = 0xCF.toChar()
    private val FULLBODY = 0xB8.toChar()
    private val LIQUID = 0xD0.toChar()
    private val BEAKER = 0xD1.toChar()
    private val TERRAIN = 0xD2.toChar()
    private val WALL = 0xD3.toChar()
    private val WIRE = 0xD4.toChar()


    override fun updateUI(delta: Float) {
        val player = ingame?.actorNowPlaying
        val hitbox = player?.hitbox

        if (hitbox != null) {
            xdelta = hitbox.canonicalX - prevPlayerX
            ydelta = hitbox.canonicalY - prevPlayerY

            prevPlayerX = hitbox.canonicalX
            prevPlayerY = hitbox.canonicalY
        }
    }

    private fun formatNanoTime(l: Long?): String {
        if (l == null) return "null"

        val sb = StringBuilder()

        l.toString().reversed().forEachIndexed { index, c ->
            if (index > 0 && index % 3 == 0)
                sb.append(' ')

            sb.append(c)
        }

        return sb.reverse().toString()
    }

    private fun Double.toIntAndFrac(textLen: Int): Pair<String,String> =
            (this.floorInt().toString().padStart(textLen)) to
                    (this.absoluteValue.times(10000.0).roundToInt() % 10000).toString().padEnd(4)

    private val gap = 14f

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val player = ingame?.actorNowPlaying

        batch.color = Color(0xFFEE88FF.toInt())

        val hitbox = player?.hitbox

        val updateCount = maxOf(1L, (App.debugTimers["Ingame.UpdateCounter"] ?: 1L) as Long)

        /**
         * First column
         */

        player?.let { player -> hitbox?.let { hitbox ->

            val (pxInt, pxFrac) = hitbox.canonicalX.toIntAndFrac(7)
            val (pyInt, pyFrac) = hitbox.canonicalY.toIntAndFrac(7)
            val (evxInt, evxFrac) = player.externalV.x.toIntAndFrac(4)
            val (evyInt, evyFrac) = player.externalV.y.toIntAndFrac(4)
            val (cvxInt, cvxFrac) = (player.controllerV?.x ?: 0.0).toIntAndFrac(4)
            val (cvyInt, cvyFrac) = (player.controllerV?.y ?: 0.0).toIntAndFrac(4)
            val (mvxInt, mvxFrac) = (xdelta / updateCount).toIntAndFrac(4)
            val (mvyInt, mvyFrac) = (ydelta / updateCount).toIntAndFrac(4)

            // TODO draw player head
            App.fontSmallNumbers.draw(batch, "X$ccG$pxInt.$pxFrac", gap + 7f*4f, line(0))
            App.fontSmallNumbers.draw(batch, "Y$ccG$pyInt.$pyFrac", gap + 7f*4f, line(1))
            batch.draw(icons.get(0,1), gap + 7f*17, line(0))


            batch.draw(icons.get(3,0), gap, line(2))
            App.fontSmallNumbers.draw(batch, "X$ccG$cvxInt.$cvxFrac", gap + 21f, line(2))
            App.fontSmallNumbers.draw(batch, "Y$ccG$cvyInt.$cvyFrac", gap + 21f, line(3))

            batch.draw(icons.get(0,1), gap + 7f*14, line(2))

            batch.draw(icons.get(2,0), gap + 7f*15, line(2))
            App.fontSmallNumbers.draw(batch, "X$ccG$evxInt.$evxFrac", gap + 7f*18, line(2))
            App.fontSmallNumbers.draw(batch, "Y$ccG$evyInt.$evyFrac", gap + 7f*18, line(3))

            batch.draw(icons.get(0,1), gap + 7f*28, line(2))

            batch.draw(icons.get(5,0), gap + 7f*29, line(2))
            App.fontSmallNumbers.draw(batch, "X$ccG$mvxInt.$mvxFrac", gap + 7f*32, line(2))
            App.fontSmallNumbers.draw(batch, "Y$ccG$mvyInt.$mvyFrac", gap + 7f*32, line(3))

            batch.draw(icons.get(1,0), gap, line(4))
            App.fontSmallNumbers.draw(batch, "${if (player.walledLeft) "$ccG" else "$ccK"}$ARROW_LEFT", gap + 7f*3, line(4) + 7)
            App.fontSmallNumbers.draw(batch, "${if (player.walledTop) "$ccG" else "$ccK"}$ARROW_UP", gap + 7f*4, line(4))
            App.fontSmallNumbers.draw(batch, "${if (player.walledBottom) "$ccG" else "$ccK"}$ARROW_DOWN", gap + 7f*4, line(5))
            App.fontSmallNumbers.draw(batch, "${if (player.walledRight) "$ccG" else "$ccK"}$ARROW_RIGHT", gap + 7f*5, line(4) + 7)
            App.fontSmallNumbers.draw(batch, "${if (player.colliding) "$ccG" else "$ccK"}$FULLBODY", gap + 7f*6, line(4) + 7)

            App.fontSmallNumbers.draw(batch, "${if (player.jumping) "$ccG" else "$ccK"}JM", gap + 7f*8, line(4))
            App.fontSmallNumbers.draw(batch, "${if (player.isJumpDown) "$ccG" else "$ccK"}KY", gap + 7f*8, line(5))

            App.fontSmallNumbers.draw(batch, "VI", gap + 7f*11, line(4))
            App.fontSmallNumbers.draw(batch, "RT", gap + 7f*11, line(5))
            App.fontSmallNumbers.draw(batch, "${if (player.downDownVirtually) "$ccG" else "$ccK"}$ARROW_DOWN", gap + 7f*13, line(5))
        }}

        batch.draw(icons.get(0,0), gap + 7f*18, line(0))
        App.fontSmallNumbers.draw(batch, "X$ccG${WorldCamera.x.toString().padStart(7)}", gap + 7f*21f, line(0))
        App.fontSmallNumbers.draw(batch, "Y$ccG${WorldCamera.y.toString().padStart(7)}", gap + 7f*21f, line(1))


        //printLine(batch, 7, "jump $ccG${player.jumpAcc}")

        val lightVal: String
        val mtX = mouseTileX.toString()
        val mtY = mouseTileY.toString()
        val valRaw = LightmapRenderer.getLight(mouseTileX, mouseTileY)
        val rawR = valRaw?.r?.times(100f)?.round()?.div(100f)
        val rawG = valRaw?.g?.times(100f)?.round()?.div(100f)
        val rawB = valRaw?.b?.times(100f)?.round()?.div(100f)
        val rawA = valRaw?.a?.times(100f)?.round()?.div(100f)

        lightVal = if (valRaw == null) "$EMDASH"
                   else "$rawR $rawG $rawB $rawA"
        printLine(batch, 8, "light@cursor $ccG$lightVal")

        try {
            world?.let {
                val wallNum = it.getTileFromWall(mouseTileX, mouseTileY)
                val tileNum = it.getTileFromTerrain(mouseTileX, mouseTileY)
                val wires = it.getAllWiresFrom(mouseTileX, mouseTileY)
                val fluid = it.getFluid(mouseTileX, mouseTileY)

                val wireCount = wires?.size?.toString() ?: "no"

                printLine(batch, 9, "tile@cursor $ccO$TERRAIN$ccG$tileNum $ccO$WALL$ccG$wallNum $ccO$WIRE$ccG($wireCount wires) $ccY($mtX,$mtY;$ccO${LandUtil.getBlockAddr(it, mouseTileX, mouseTileY)}$ccY)")
                printLine(batch, 10, "fluid@cursor $ccO$LIQUID$ccG${fluid.type.value} $ccO$BEAKER$ccG${fluid.amount}f")

                printLineColumn(batch, 2, 5, "Time $ccG${it.worldTime.todaySeconds.toString().padStart(5, '0')}" +
                                             " (${it.worldTime.getFormattedTime()})")
            }
        }
        catch (e: NullPointerException) {}


        // print time
        var dbgCnt = 12
        App.debugTimers.forEach { t, u ->
            printLine(batch, dbgCnt, "$ccM$t $ccG${formatNanoTime(u as? Long)}$ccY ns")
            dbgCnt++
        }


        /**
         * Second column
         */

        //printLineColumn(batch, 2, 1, "VSync $ccG" + Terrarum.appgc.isVSyncRequested)
        //printLineColumn(batch, 2, 2, "Env colour temp $ccG" + FeaturesDrawer.colTemp)

        if (player != null) {
            printLineColumn(batch, 2, 6, "Mass $ccG${player.mass}")
            printLineColumn(batch, 2, 7, "noClip $ccG${player.isNoClip}")
        }

        /*drawHistogram(batch, LightmapRenderer.histogram,
                AppLoader.terrarumAppConfig.screenW - histogramW - TinyAlphNum.W * 2,
                AppLoader.terrarumAppConfig.screenH - histogramH - TinyAlphNum.H * 4
        )*/ // histogram building is currently bugged

        batch.color = Color.WHITE

        val gamepad = (Terrarum.ingame as? TerrarumIngame)?.ingameController?.gamepad
        if (gamepad != null) {
            drawGamepadAxis(gamepad, batch,
                    gamepad.getAxis(App.getConfigInt("control_gamepad_axislx")),
                    gamepad.getAxis(App.getConfigInt("control_gamepad_axisly")),
                    App.scr.width - 128 - TinyAlphNum.W * 2,
                    line(3).toInt()
            )
        }

        /**
         * Top right
         */

        // memory pressure
        App.fontSmallNumbers.draw(batch, "${ccY}MEM ", (App.scr.width - 23 * TinyAlphNum.W - 2).toFloat(), line(1))
        // thread count
        App.fontSmallNumbers.draw(batch, "${ccY}CPUs${if (App.MULTITHREAD) ccG else ccR}${App.THREAD_COUNT.toString().padStart(2, ' ')}",
                (App.scr.width - 2 - 8 * TinyAlphNum.W).toFloat(), line(2))

        // memory texts
        App.fontSmallNumbers.draw(batch, "${Terrarum.memJavaHeap}M",
                (App.scr.width - 19 * TinyAlphNum.W - 2).toFloat(), line(1))
        App.fontSmallNumbers.draw(batch, "/${Terrarum.memNativeHeap}M/",
                (App.scr.width - 14 * TinyAlphNum.W - 2).toFloat(), line(1))
        App.fontSmallNumbers.draw(batch, "${Terrarum.memXmx}M",
                (App.scr.width - 7 * TinyAlphNum.W - 2).toFloat(), line(1))
        // FPS count
        App.fontSmallNumbers.draw(batch, "${ccY}FPS${ccG}${Gdx.graphics.framesPerSecond.toString().padStart(3, ' ')}",
                (App.scr.width - 3 - 15 * TinyAlphNum.W).toFloat(), line(2))
        // global render counter
        App.fontSmallNumbers.draw(batch, "${ccO}R${App.GLOBAL_RENDER_TIMER.toString().padStart(9, ' ')}",
                (App.scr.width - 35 * TinyAlphNum.W - 2).toFloat(), line(1))
        (ingame as? TerrarumIngame)?.let {
            // global update counter (if applicable)
            App.fontSmallNumbers.draw(batch, "${ccO}U${it.WORLD_UPDATE_TIMER.toString().padStart(9, ' ')}",
                    (App.scr.width - 35 * TinyAlphNum.W - 2).toFloat(), line(2))
        }
        /**
         * Bottom left
         */

        if (ingame != null) {
            App.fontSmallNumbers.draw(batch, "${ccY}Actors total $ccG${ingame!!.actorContainerActive.size + ingame!!.actorContainerInactive.size}",
                    TinyAlphNum.W * 2f, App.scr.height - TinyAlphNum.H * 2f)
            App.fontSmallNumbers.draw(batch, "${ccY}Active $ccG${ingame!!.actorContainerActive.size}",
                    (TinyAlphNum.W * 2 + 17 * 8).toFloat(), App.scr.height - TinyAlphNum.H * 2f)
            App.fontSmallNumbers.draw(batch, "${ccY}Dormant $ccG${ingame!!.actorContainerInactive.size}",
                    (TinyAlphNum.W * 2 + 28 * 8).toFloat(), App.scr.height - TinyAlphNum.H * 2f)
            if (ingame is TerrarumIngame) {
                App.fontSmallNumbers.draw(batch, "${ccM}Particles $ccG${(ingame as TerrarumIngame).particlesActive}",
                        (TinyAlphNum.W * 2 + 41 * 8).toFloat(), App.scr.height - TinyAlphNum.H * 2f)
            }
        }

        App.fontSmallNumbers.draw(batch, "${ccY}Actors rendering $ccG${IngameRenderer.renderingActorsCount}",
                TinyAlphNum.W * 2f, App.scr.height - TinyAlphNum.H * 3f)
        App.fontSmallNumbers.draw(batch, "${ccY}UIs rendering $ccG${IngameRenderer.renderingUIsCount}",
                TinyAlphNum.W * 2f + (21 * 8), App.scr.height - TinyAlphNum.H * 3f)

        /**
         * Bottom right
         */

        // processor and renderer
        App.fontSmallNumbers.draw(batch, "$ccY$totalHardwareName",
                (App.scr.width - (totalHardwareName.length + 2) * TinyAlphNum.W).toFloat(), App.scr.height - TinyAlphNum.H * 2f)
    }

    private val processorName = App.processor.replace(Regex(""" Processor|( CPU)? @ [0-9.]+GHz"""), "") + if (App.is32BitJVM) " (32-bit)" else ""
    private val rendererName = App.renderer
    private val totalHardwareName = "$processorName  $rendererName"

    private fun printLine(batch: SpriteBatch, l: Int, s: String) {
        App.fontSmallNumbers.draw(batch,
                s, gap, line(l)
        )
    }

    private fun printLineColumn(batch: SpriteBatch, col: Int, row: Int, s: String) {
        App.fontSmallNumbers.draw(batch,
                s, column(col), line(row)
        )
    }


    val histogramW = 256
    val histogramH = 256

    private fun drawHistogram(batch: SpriteBatch, histogram: LightmapRenderer.Histogram, x: Int, y: Int) {
        val uiColour = Color(0x000000_80.toInt())
        val barR = Color(0xFF0000_FF.toInt())
        val barG = Color(0x00FF00_FF.toInt())
        val barB = Color(0x0000FF_FF.toInt())
        val barA = Color.WHITE
        val barColour = arrayOf(barR, barG, barB, barA)
        val w = histogramW.toFloat()
        val h = histogramH.toFloat()
        val halfh = h / 2f
        val range = histogram.range
        val histogramMax = histogram.screen_tiles.toFloat()

        batch.color = uiColour
        Toolkit.fillArea(batch, x.toFloat(), y.toFloat(), w.plus(1), h)
        batch.color = Color.GRAY
        App.fontSmallNumbers.draw(batch, "0", x.toFloat(), y.toFloat() + h + 2)
        App.fontSmallNumbers.draw(batch, "255", x.toFloat() + w + 1 - 8 * 3, y.toFloat() + h + 2)
        App.fontSmallNumbers.draw(batch, "Histogramme", x + w / 2 - 5.5f * 8, y.toFloat() + h + 2)

        blendScreen(batch)
        for (c in 0..3) {
            for (i in 0..255) {
                var histogram_value = if (i == 255) 0 else histogram.get(c)[i]
                if (i == 255) {
                    for (k in 255..range - 1) {
                        histogram_value += histogram.get(c)[k]
                    }
                }

                val bar_x = x + (w / w.minus(1f)) * i.toFloat()
                val bar_h = halfh * (histogram_value.toFloat() / histogramMax).sqrt()
                val bar_y = if (c == 3) y + halfh else y + h
                val bar_w = 1f

                batch.color = barColour[c]
                Toolkit.fillArea(batch, bar_x, bar_y, bar_w, -bar_h)
            }
        }
        blendNormal(batch)
    }

    private fun drawGamepadAxis(gamepad: TerrarumController, batch: SpriteBatch, axisX: Float, axisY: Float, uiX: Int, uiY: Int) {
        val uiColour = ItemSlotImageFactory.CELLCOLOUR_BLACK
        val deadzoneColour = Color(0xaa0000aa.toInt())
        val w = 128f
        val h = 128f
        val halfW = w / 2f
        val halfH = h / 2f

        val pointDX = axisX * halfW
        val pointDY = -axisY * halfH

        val deadzone = App.gamepadDeadzone

        blendNormal(batch)

        batch.end()
        gdxSetBlendNormal()
        Terrarum.inShapeRenderer {
            it.color = uiColour
            it.rect(uiX.toFloat(), App.scr.height - uiY.toFloat(), w, -h)
            it.color = deadzoneColour
            it.rect(uiX + halfW - (halfW * deadzone), App.scr.height - (uiY + halfH - halfH * deadzone), w * deadzone, -h * deadzone)
            it.color = Color.WHITE
            it.line(uiX + halfW, App.scr.height - (uiY + halfH), uiX + halfW + pointDX, App.scr.height - (uiY + halfH + pointDY))
            it.color = Color.GRAY
        }
        batch.begin()

        App.fontSmallNumbers.draw(batch, gamepad.getName(), App.scr.width - (gamepad.getName().length + 2f) * TinyAlphNum.W, uiY.toFloat() + h + 2)

    }

    private fun line(i: Int): Float = gap + i * TinyAlphNum.H.toFloat()
    private fun column(i: Int): Float = gap + 300f * (i - 1)

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        icons.dispose()
    }
}