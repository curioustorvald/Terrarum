package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign

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
    private val back = Texture(Gdx.files.internal("assets/graphics/gui/debug_window_background.tga"))
    private val back2 = Texture(Gdx.files.internal("assets/graphics/gui/debug_window_background2.tga"))

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
    private val MASS = 0xD5.toChar()
    private val SOL = 0xD6.toChar()
    private val TAU = 0xD7.toChar()
    private val HEIGHT = 0xC7.toChar()
    private val WIDTH = 0xCD.toChar()

    private val KEY_TIMERS = Input.Keys.U

    private var showTimers = false

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

    private infix fun Double.pow(b: Double) = Math.pow(this, b)

    private fun Double?.toIntAndFrac(intLen: Int, fracLen: Int = 4): String =
            if (this == null) "null" else if (this.isNaN()) "NaN" else if (this.isInfinite()) "${if (this.sign >= 0) '+' else '-'}Inf" else
                "${this.toInt().toString().padStart(intLen)}." +
            (10.0 pow fracLen.toDouble()).let { d -> (this.absoluteValue.times(d) % d).toInt().toString().padEnd(fracLen) }

    private val gap = 14f

    private val pxyX = 0; private val pxyY = 0
    private val cxyX = 23; private val cxyY = 0
    private val jX = 35; private val jY = 0

    private val cvX = 0; private val cvY = 2
    private val evX = 14; private val evY = 2
    private val mvX = 28; private val mvY = 2
    private val sol = 42;

    private val tileCursX = 0; private val tileCursY = 4

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val windowWidth = Toolkit.drawWidth
        val player = ingame?.actorNowPlaying
        val hitbox = player?.hitbox
        val updateCount = max(1L, (App.debugTimers["Ingame.UpdateCounter"] ?: 1L))

        /**
         * Top Left
         */

        batch.color = Color(1f, 1f, 1f, 0.65f)
        batch.draw(back, gap - 4f, gap - 4f - 1f)
        batch.draw(back2, windowWidth - back2.width - (gap - 4f), gap - 4f - 1f)

        showTimers = showTimers xor Gdx.input.isKeyJustPressed(KEY_TIMERS)

        batch.color = Color(0xFFEE88FF.toInt())

        player?.let { player -> hitbox?.let { hitbox ->

            val px = hitbox.canonicalX.toIntAndFrac(7)
            val py = hitbox.canonicalY.toIntAndFrac(7)
            val evx = player.externalV.x.toIntAndFrac(4)
            val evy = player.externalV.y.toIntAndFrac(4)
            val cvx = (player.controllerV?.x ?: 0.0).toIntAndFrac(4)
            val cvy = (player.controllerV?.y ?: 0.0).toIntAndFrac(4)
            val mvx = (xdelta / updateCount).toIntAndFrac(4)
            val mvy = (ydelta / updateCount).toIntAndFrac(4)

            // TODO draw player head
            App.fontSmallNumbers.draw(batch, "X$ccO${hitbox.canonicalX.div(TILE_SIZE).toInt().toString().padStart(6)}$ccG$px", gap + 7f*(pxyX + 3), line(pxyY))
            App.fontSmallNumbers.draw(batch, "Y$ccO${hitbox.canonicalY.div(TILE_SIZE).toInt().toString().padStart(6)}$ccG$py", gap + 7f*(pxyX + 3), line(pxyY+1))
//            batch.draw(icons.get(0,1), gap + 7f*(cxyX - 1), line(pxyY))

            // camera info

//            batch.draw(icons.get(0,1), gap + 7f*(jX - 1), line(jY))

            batch.draw(icons.get(1,0), gap + 7f*jX, line(jY))
            App.fontSmallNumbers.draw(batch, "${if (player.walledLeft) "$ccG" else "$ccK"}$ARROW_LEFT", gap + 7f*(jX + 3), line(jY) + 7)
            App.fontSmallNumbers.draw(batch, "${if (player.walledTop) "$ccG" else "$ccK"}$ARROW_UP", gap + 7f*(jX + 4), line(jY))
            App.fontSmallNumbers.draw(batch, "${if (player.walledBottom) "$ccG" else "$ccK"}$ARROW_DOWN", gap + 7f*(jX + 4), line(jY+1))
            App.fontSmallNumbers.draw(batch, "${if (player.walledRight) "$ccG" else "$ccK"}$ARROW_RIGHT", gap + 7f*(jX + 5), line(jY) + 7)
            App.fontSmallNumbers.draw(batch, "${if (player.colliding) "$ccG" else "$ccK"}$FULLBODY", gap + 7f*(jX + 6), line(jY) + 7)

            App.fontSmallNumbers.draw(batch, "${if (player.jumping) "$ccG" else "$ccK"}JM", gap + 7f*(jX + 8), line(jY))
            App.fontSmallNumbers.draw(batch, "${if (player.isJumpDown) "$ccG" else "$ccK"}KY", gap + 7f*(jX + 8), line(jY+1))

            App.fontSmallNumbers.draw(batch, "${if (player.downDownVirtually) "$ccG" else "$ccK"}$ARROW_DOWN", gap + 7f*(jX + 11), line(jY+1))

            App.fontSmallNumbers.draw(batch, "$WIDTH$ccG${player.hitbox.width.toString().padEnd(5).substring(0,5).trim()}$ccY$HEIGHT$ccG${player.hitbox.height.toString().padEnd(5).substring(0,5)}", gap + 7f*(jX + 13), line(jY))
            App.fontSmallNumbers.draw(batch, "$MASS$ccG${player.mass.toString().padEnd(8).substring(0,8)}", gap + 7f*(jX + 13), line(jY+1))



            batch.draw(icons.get(3,0), gap + 7f*cvX, line(cvY))
            App.fontSmallNumbers.draw(batch, "X$ccG$cvx", gap + 7f*(cvX + 3), line(cvY))
            App.fontSmallNumbers.draw(batch, "Y$ccG$cvy", gap + 7f*(cvX + 3), line(cvY + 1))

//            batch.draw(icons.get(0,1), gap + 7f*(evX - 1), line(evY))

            batch.draw(icons.get(2,0), gap + 7f*evX, line(evY))
            App.fontSmallNumbers.draw(batch, "X$ccG$evx", gap + 7f*(evX + 3), line(evY))
            App.fontSmallNumbers.draw(batch, "Y$ccG$evy", gap + 7f*(evX + 3), line(evY + 1))

//            batch.draw(icons.get(0,1), gap + 7f*(mvX - 1), line(mvY))

            batch.draw(icons.get(5,0), gap + 7f*mvX, line(mvY))
            App.fontSmallNumbers.draw(batch, "X$ccG$mvx", gap + 7f*(mvX + 3), line(mvY))
            App.fontSmallNumbers.draw(batch, "Y$ccG$mvy", gap + 7f*(mvX + 3), line(mvY + 1))

        }}

        batch.draw(icons.get(0,0), gap + 7f*cxyX, line(cxyY))
        App.fontSmallNumbers.draw(batch, "X$ccG${WorldCamera.x.toString().padStart(7)}", gap + 7f*(cxyX + 3), line(cxyY))
        App.fontSmallNumbers.draw(batch, "Y$ccG${WorldCamera.y.toString().padStart(7)}", gap + 7f*(cxyX + 3), line(cxyY+1))


        // sun and weather
        val soldeg = WeatherMixer.forceSolarElev ?: world?.worldTime?.solarElevationDeg
        val soldegStr = (soldeg ?: 0.0).toIntAndFrac(3,2)
        val soldegNeg = ((soldeg ?: 0.0) >= 0.0).toInt()
        val turbidity = WeatherMixer.forceTurbidity ?: WeatherMixer.turbidity

        val soldegCol = if (WeatherMixer.forceSolarElev != null) ccO else ccG
        val turbCol = if (WeatherMixer.forceTurbidity != null) ccO else ccG

        App.fontSmallNumbers.draw(batch, "$SOL $soldegCol$soldegStr", gap + 7f*(sol), line(mvY))
        App.fontSmallNumbers.draw(batch, "$TAU   $turbCol$turbidity", gap + 7f*(sol), line(mvY + 1))



        try {
            world?.let {
                val valRaw = LightmapRenderer.getLight(mouseTileX, mouseTileY)
                val rawR = valRaw?.r?.toDouble().toIntAndFrac(1,3)
                val rawG = valRaw?.g?.toDouble().toIntAndFrac(1,3)
                val rawB = valRaw?.b?.toDouble().toIntAndFrac(1,3)
                val rawA = valRaw?.a?.toDouble().toIntAndFrac(1,3)

                val wallNum = it.getTileFromWall(mouseTileX, mouseTileY)
                val tileNum = it.getTileFromTerrain(mouseTileX, mouseTileY)
                val wires = it.getAllWiresFrom(mouseTileX, mouseTileY)
                val fluid = it.getFluid(mouseTileX, mouseTileY)
                val wireCount = wires.first?.size?.toString() ?: "no"

                App.fontSmallNumbers.draw(batch, "$ccO$TERRAIN$ccG$tileNum", gap + 7f*(tileCursX + 3), line(tileCursY))
                App.fontSmallNumbers.draw(batch, "$ccO$WALL$ccG$wallNum", gap + 7f*(tileCursX + 3), line(tileCursY + 1))
                App.fontSmallNumbers.draw(batch, "$ccO$LIQUID$ccG${fluid.type.value.toString().padEnd(3)}$ccO$BEAKER$ccG${fluid.amount}f", gap + 7f*(tileCursX + 3), line(tileCursY + 2))
                App.fontSmallNumbers.draw(batch, "$ccO$WIRE$ccG$wireCount ${ccY}X$ccO$mouseTileX ${ccY}Y$ccO$mouseTileY", gap + 7f*(tileCursX + 3), line(tileCursY + 3))
                App.fontSmallNumbers.draw(batch, "$ccR$rawR $ccG$rawG $ccB$rawB $ccW$rawA", gap + 7f*(tileCursX + 3), line(tileCursY + 4))

                batch.draw(icons.get(4,0), gap + 7f*tileCursX, line(tileCursY + 1) + 7)
            }
        }
        catch (e: NullPointerException) {}


        batch.color = Color.WHITE

        val gamepad = (Terrarum.ingame as? TerrarumIngame)?.ingameController?.gamepad
        if (gamepad != null) {
            drawGamepadAxis(gamepad, batch,
                    gamepad.getAxis(App.getConfigInt("control_gamepad_axislx")),
                    gamepad.getAxis(App.getConfigInt("control_gamepad_axisly")),
                    windowWidth - 128 - TinyAlphNum.W * 2,
                    line(3).toInt()
            )
        }


        // print time
        if (showTimers) {
            var dbgCnt = 10
            App.debugTimers.forEach { t, u ->
                printLine(batch, dbgCnt, "$ccM$t $ccG${formatNanoTime(u as? Long)}$ccY ns")
                dbgCnt++
            }
        }


        /**
         * Top right
         */

        // memory pressure
        App.fontSmallNumbers.draw(batch, "${ccY}MEM ", (windowWidth - 25 * TinyAlphNum.W).toFloat(), line(0))
        // thread count
        App.fontSmallNumbers.draw(batch, "${ccY}CPUs${if (App.MULTITHREAD) ccG else ccR}${App.THREAD_COUNT.toString().padStart(2, ' ')}",
                (windowWidth - 8 * TinyAlphNum.W).toFloat(), line(1))

        // memory texts
        App.fontSmallNumbers.draw(batch, "${ccO}H$ccG${Terrarum.memJavaHeap}${ccY}M",
                (windowWidth - 21 * TinyAlphNum.W).toFloat(), line(0))
        App.fontSmallNumbers.draw(batch, "${ccO}U$ccG${Terrarum.memUnsafe}${ccY}M",
                (windowWidth - 14 * TinyAlphNum.W).toFloat(), line(0))
        App.fontSmallNumbers.draw(batch, "${ccO}X$ccG${Terrarum.memXmx}${ccY}M",
                (windowWidth - 8 * TinyAlphNum.W).toFloat(), line(0))
        // FPS count
        App.fontSmallNumbers.draw(batch, "${ccY}FPS${ccG}${Gdx.graphics.framesPerSecond.toString().padStart(3, ' ')}",
                (windowWidth - 15 * TinyAlphNum.W).toFloat(), line(1))
        // global render counter
        App.fontSmallNumbers.draw(batch, "$ccO${"R ${App.GLOBAL_RENDER_TIMER}".padStart(10).substring(0,10)}",
                (windowWidth - 12 * TinyAlphNum.W).toFloat(), line(2))
        (ingame as? TerrarumIngame)?.let {
            // global update counter (if applicable)
            App.fontSmallNumbers.draw(batch, "$ccO${"U ${it.WORLD_UPDATE_TIMER}".padStart(10).substring(0,10)}",
                    (windowWidth - 12 * TinyAlphNum.W).toFloat(), line(3))
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
                (windowWidth - (totalHardwareName.length+2) * TinyAlphNum.W).toFloat(), App.scr.height - TinyAlphNum.H * 2f)
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
        blendNormalStraightAlpha(batch)
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

        blendNormalStraightAlpha(batch)

        /*batch.end()
        gdxBlendNormalStraightAlpha()
        Terrarum.inShapeRenderer {
            it.color = uiColour
            it.rect(uiX.toFloat(), App.scr.height - uiY.toFloat(), w, -h)
            it.color = deadzoneColour
            it.rect(uiX + halfW - (halfW * deadzone), App.scr.height - (uiY + halfH - halfH * deadzone), w * deadzone, -h * deadzone)
            it.color = Color.WHITE
            it.line(uiX + halfW, App.scr.height - (uiY + halfH), uiX + halfW + pointDX, App.scr.height - (uiY + halfH + pointDY))
            it.color = Color.GRAY
        }
        batch.begin()*/

        App.fontSmallNumbers.draw(batch, gamepad.getName(), Toolkit.drawWidth - (gamepad.getName().length + 2f) * TinyAlphNum.W, uiY.toFloat() + h + 2)

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
        back.dispose()
        back2.dispose()
    }
}