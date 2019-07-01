package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum.mouseTileX
import net.torvald.terrarum.Terrarum.mouseTileY
import net.torvald.terrarum.controller.TerrarumController
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 2016-03-14.
 */
class BasicDebugInfoWindow : UICanvas() {

    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH

    override var openCloseTime: Float = 0f

    private var prevPlayerX = 0.0
    private var prevPlayerY = 0.0

    private var xdelta = 0.0
    private var ydelta = 0.0

    private val ingame: IngameInstance?
        get() = Terrarum.ingame

    private val world: GameWorld?
        get() = Terrarum.ingame?.world
    private val world2: GameWorldExtension?
        get() = Terrarum.ingame?.world as? GameWorldExtension?


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

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val player = ingame?.actorNowPlaying

        batch.color = Color(0xFFEE88FF.toInt())

        val hitbox = player?.hitbox

        /**
         * First column
         */

        if (player != null) {

            printLineColumn(batch, 1, 1, "startX "
                                         + ccG
                                         + "${hitbox?.startX}"
                                         + " ("
                                         + "${(hitbox?.startX?.div(CreateTileAtlas.TILE_SIZE))?.toInt()}"
                                         + ")")
            printLineColumn(batch, 2, 1, "endX "
                                         + ccG
                                         + "${hitbox?.endX}"
                                         + " ("
                                         + "${(hitbox?.endX?.div(CreateTileAtlas.TILE_SIZE))?.toInt()}"
                                         + ")")
            printLineColumn(batch, 3, 1, "camX "
                                         + ccG
                                         + "${WorldCamera.x}")
            printLineColumn(batch, 1, 2, "startY "
                                         + ccG
                                         + "${hitbox?.startY}"
                                         + " ("
                                         + "${(hitbox?.startY?.div(CreateTileAtlas.TILE_SIZE))?.toInt()}"
                                         + ")")
            printLineColumn(batch, 2, 2, "endY "
                                         + ccG
                                         + "${hitbox?.endY}"
                                         + " ("
                                         + "${(hitbox?.endY?.div(CreateTileAtlas.TILE_SIZE))?.toInt()}"
                                         + ")")
            printLineColumn(batch, 3, 2, "camY "
                                         + ccG
                                         + "${WorldCamera.y}")

            printLine(batch, 3, "veloX reported $ccG${player.externalV.x}")
            printLine(batch, 4, "veloY reported $ccG${player.externalV.y}")

            printLine(batch, 5, "p_WalkX $ccG${player.controllerV?.x}")
            printLine(batch, 6, "p_WalkY $ccG${player.controllerV?.y}")

            printLineColumn(batch, 2, 3, "veloX measured $ccG${xdelta}")
            printLineColumn(batch, 2, 4, "veloY measured $ccG${ydelta}")

            printLineColumn(batch, 1, 7,
                    "walled " +
                    "${if (player.walledLeft) "$ccR" else "$ccG"}L" +
                    "${if (player.walledBottom) "$ccR" else "$ccG"}${0x1F.toChar()}" +
                    "${if (player.walledTop) "$ccR" else "$ccG"}${0x1E.toChar()}" +
                    "${if (player.walledRight) "$ccR" else "$ccG"}R" +
                    "${if (player.colliding) "$ccR" else "$ccG"}${0x08.toChar()}"
            )
        }



        //printLine(batch, 7, "jump $ccG${player.jumpAcc}")

        val lightVal: String
        val mtX = mouseTileX.toString()
        val mtY = mouseTileY.toString()
        val valRaw = LightmapRenderer.getLight(mouseTileX, mouseTileY)
        val rawR = valRaw?.r?.times(100f)?.round()?.div(100f)
        val rawG = valRaw?.g?.times(100f)?.round()?.div(100f)
        val rawB = valRaw?.b?.times(100f)?.round()?.div(100f)
        val rawA = valRaw?.a?.times(100f)?.round()?.div(100f)

        lightVal = if (valRaw == null) "—"
                   else "$rawR $rawG $rawB $rawA"
        printLine(batch, 8, "light@cursor $ccG$lightVal")

        if (ingame != null) {
            val wallNum = ingame!!.world.getTileFromWall(mouseTileX, mouseTileY)
            val tileNum = ingame!!.world.getTileFromTerrain(mouseTileX, mouseTileY)
            val wireNum = ingame!!.world.getWiringBlocks(mouseTileX, mouseTileY)
            val fluid = ingame!!.world.getFluid(mouseTileX, mouseTileY)

            printLine(batch, 9, "tile@cursor ${ccO}W$ccG$wallNum ${ccO}T$ccG$tileNum ${ccO}C$ccG${wireNum.toString(2)} $ccY($mtX, $mtY)")
            printLine(batch, 10, "fluid@cursor ${ccO}Type $ccG${fluid.type.value} ${ccO}Fill $ccG${fluid.amount}f")

        }


        // print time
        var dbgCnt = 12
        AppLoader.debugTimers.forEach { t, u ->
            printLine(batch, dbgCnt, "$ccM$t $ccG${formatNanoTime(u as? Long)}$ccY ns")
            dbgCnt++
        }


        /**
         * Second column
         */

        //printLineColumn(batch, 2, 1, "VSync $ccG" + Terrarum.appgc.isVSyncRequested)
        //printLineColumn(batch, 2, 2, "Env colour temp $ccG" + FeaturesDrawer.colTemp)

        if (world != null) {
            printLineColumn(batch, 2, 5, "Time $ccG${world2?.worldTime?.todaySeconds.toString().padStart(5, '0')}" +
                                         " (${world2?.worldTime?.getFormattedTime()})")
        }

        if (player != null) {
            printLineColumn(batch, 2, 6, "Mass $ccG${player.mass}")

            printLineColumn(batch, 2, 7, "noClip $ccG${player.isNoClip}")
        }

        /*drawHistogram(batch, LightmapRenderer.histogram,
                Terrarum.WIDTH - histogramW - TinyAlphNum.W * 2,
                Terrarum.HEIGHT - histogramH - TinyAlphNum.H * 4
        )*/ // histogram building is currently bugged

        batch.color = Color.WHITE

        val gamepad = (Terrarum.ingame as? TerrarumIngame)?.ingameController?.gamepad
        if (gamepad != null) {
            drawGamepadAxis(gamepad, batch,
                    gamepad.getAxis(AppLoader.getConfigInt("gamepadaxislx")),
                    gamepad.getAxis(AppLoader.getConfigInt("gamepadaxisly")),
                    Terrarum.WIDTH - 128 - TinyAlphNum.W * 2,
                    line(3).toInt()
            )
        }

        /**
         * Top right
         */

        // memory pressure
        AppLoader.fontSmallNumbers.draw(batch, "${ccY}MEM ", (Terrarum.WIDTH - 23 * TinyAlphNum.W - 2).toFloat(), line(1))
        // thread count
        AppLoader.fontSmallNumbers.draw(batch, "${ccY}CPUs${if (AppLoader.MULTITHREAD) ccG else ccR}${AppLoader.THREADS.toString().padStart(2, ' ')}",
                (Terrarum.WIDTH - 2 - 8 * TinyAlphNum.W).toFloat(), line(2))

        // memory texts
        AppLoader.fontSmallNumbers.draw(batch, "${Terrarum.memJavaHeap}M",
                (Terrarum.WIDTH - 19 * TinyAlphNum.W - 2).toFloat(), line(1))
        AppLoader.fontSmallNumbers.draw(batch, "/${Terrarum.memNativeHeap}M/",
                (Terrarum.WIDTH - 14 * TinyAlphNum.W - 2).toFloat(), line(1))
        AppLoader.fontSmallNumbers.draw(batch, "${Terrarum.memXmx}M",
                (Terrarum.WIDTH - 7 * TinyAlphNum.W - 2).toFloat(), line(1))
        // FPS count
        AppLoader.fontSmallNumbers.draw(batch, "${ccY}FPS${ccG}${Gdx.graphics.framesPerSecond.toString().padStart(3, ' ')}",
                (Terrarum.WIDTH - 3 - 15 * TinyAlphNum.W).toFloat(), line(2))

        /**
         * Bottom left
         */

        if (ingame != null) {
            AppLoader.fontSmallNumbers.draw(batch, "${ccY}Actors total $ccG${ingame!!.actorContainerActive.size + ingame!!.actorContainerInactive.size}",
                    TinyAlphNum.W * 2f, Terrarum.HEIGHT - TinyAlphNum.H * 2f)
            AppLoader.fontSmallNumbers.draw(batch, "${ccY}Active $ccG${ingame!!.actorContainerActive.size}",
                    (TinyAlphNum.W * 2 + 17 * 8).toFloat(), Terrarum.HEIGHT - TinyAlphNum.H * 2f)
            AppLoader.fontSmallNumbers.draw(batch, "${ccY}Dormant $ccG${ingame!!.actorContainerInactive.size}",
                    (TinyAlphNum.W * 2 + 28 * 8).toFloat(), Terrarum.HEIGHT - TinyAlphNum.H * 2f)
            if (ingame is TerrarumIngame) {
                AppLoader.fontSmallNumbers.draw(batch, "${ccM}Particles $ccG${(ingame as TerrarumIngame).particlesActive}",
                        (TinyAlphNum.W * 2 + 41 * 8).toFloat(), Terrarum.HEIGHT - TinyAlphNum.H * 2f)
            }
        }

        AppLoader.fontSmallNumbers.draw(batch, "${ccY}Actors rendering $ccG${IngameRenderer.renderingActorsCount}",
                TinyAlphNum.W * 2f, Terrarum.HEIGHT - TinyAlphNum.H * 3f)
        AppLoader.fontSmallNumbers.draw(batch, "${ccY}UIs rendering $ccG${IngameRenderer.renderingUIsCount}",
                TinyAlphNum.W * 2f + (21 * 8), Terrarum.HEIGHT - TinyAlphNum.H * 3f)

        /**
         * Bottom right
         */

        // processor and renderer
        AppLoader.fontSmallNumbers.draw(batch, "$ccY$totalHardwareName",
                (Terrarum.WIDTH - (totalHardwareName.length + 2) * TinyAlphNum.W).toFloat(), Terrarum.HEIGHT - TinyAlphNum.H * 2f)
    }

    private val processorName = AppLoader.processor.replace(Regex(""" Processor|( CPU)? @ [0-9.]+GHz"""), "") + if (AppLoader.is32BitJVM) " (32-bit)" else ""
    private val rendererName = AppLoader.renderer
    private val totalHardwareName = "$processorName  $rendererName"

    private fun printLine(batch: SpriteBatch, l: Int, s: String) {
        AppLoader.fontSmallNumbers.draw(batch,
                s, TinyAlphNum.W * 2f, line(l)
        )
    }

    private fun printLineColumn(batch: SpriteBatch, col: Int, row: Int, s: String) {
        AppLoader.fontSmallNumbers.draw(batch,
                s, (TinyAlphNum.W * 2f + column(col)), line(row)
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
        batch.fillRect(x.toFloat(), y.toFloat(), w.plus(1), h)
        batch.color = Color.GRAY
        AppLoader.fontSmallNumbers.draw(batch, "0", x.toFloat(), y.toFloat() + h + 2)
        AppLoader.fontSmallNumbers.draw(batch, "255", x.toFloat() + w + 1 - 8 * 3, y.toFloat() + h + 2)
        AppLoader.fontSmallNumbers.draw(batch, "Histogramme", x + w / 2 - 5.5f * 8, y.toFloat() + h + 2)

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
                batch.fillRect(bar_x, bar_y, bar_w, -bar_h)
            }
        }
        blendNormal(batch)
    }

    private fun drawGamepadAxis(gamepad: TerrarumController, batch: SpriteBatch, axisX: Float, axisY: Float, uiX: Int, uiY: Int) {
        val uiColour = ItemSlotImageFactory.CELLCOLOUR_BLACK
        val w = 128f
        val h = 128f
        val halfW = w / 2f
        val halfH = h / 2f

        val pointDX = axisX * halfW
        val pointDY = -axisY * halfH

        blendNormal(batch)

        batch.end()
        gdxSetBlendNormal()
        Terrarum.inShapeRenderer {
            it.color = uiColour
            it.rect(uiX.toFloat(), Terrarum.HEIGHT - uiY.toFloat(), w, -h)
            it.color = Color.WHITE
            it.line(uiX + halfW, Terrarum.HEIGHT - (uiY + halfH), uiX + halfW + pointDX, Terrarum.HEIGHT - (uiY + halfH + pointDY))
            it.color = Color.GRAY
        }
        batch.begin()

        AppLoader.fontSmallNumbers.draw(batch, gamepad.getName(), Terrarum.WIDTH - (gamepad.getName().length + 2f) * TinyAlphNum.W, uiY.toFloat() + h + 2)

    }

    private fun line(i: Int): Float = i * TinyAlphNum.H.toFloat()

    private fun column(i: Int): Float = 300f * (i - 1)

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}