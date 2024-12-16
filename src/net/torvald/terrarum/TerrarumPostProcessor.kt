package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.getbatterystatus.GetBatteryStatus
import net.torvald.random.HQRNG
import net.torvald.terrarum.App.IS_DEVELOPMENT_BUILD
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.savegame.toHex
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unsafe.UnsafeHelper
import java.time.ZonedDateTime

/**
 * Must be called by the App Loader
 *
 * We recommened most of the user interfaces to be contained within the UI Area which has aspect ratio of 3:2.
 */
object TerrarumPostProcessor : Disposable {

    private lateinit var batch: FlippingSpriteBatch // not nulling to save some lines of code
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera

    private lateinit var lutTex: Texture

    private var init = false

    private lateinit var outFBO: FrameBuffer

    fun reloadLUT(filename: String) {
        lutTex = Texture(Gdx.files.internal("assets/clut/$filename"))
    }

    private val defaultResCol = Color(0x66ffff66)
    private val safeAreaCol = Color(0xffffff66.toInt())
    private val safeAreaCol2 = Color(0xffffff44.toInt())
    private val currentResCol = Color(0xfff066_88.toInt())
    private val noGoAreaCol = Color(0xff444466.toInt())

    internal val debugUI = BasicDebugInfoWindow()

    private val functionRowHelper = Texture(Gdx.files.internal("assets/graphics/function_row_help.png"))

    private val batteryTex = TextureRegionPack(Gdx.files.internal("assets/graphics/gui/fullscreen_bat_ind.tga"), 23, 14)

    private val shaderPostDither = ShaderMgr["postDither"]
    private val shaderPostNoDither = ShaderMgr["postNoDither"]

    private val recommendRatio = 1.5f

    /*private val testfill = Texture("./assets/test_fill.tga").also {
        it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }*/

    private val shaderQuant = mapOf(
            8 to 255f,
            10 to 1023f,
            12 to 4095f,
            14 to 16383f,
            15 to 32767f,
            16 to 65535f
    )

    init {
        App.disposables.add(this)
    }

    fun resize(w: Int, h: Int) {
        if (::outFBO.isInitialized) outFBO.tryDispose()
        outFBO = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        functionRowHelper.dispose()
        if (::lutTex.isInitialized) lutTex.tryDispose()
        if (::outFBO.isInitialized) outFBO.dispose()
//        testfill.dispose()
    }

    private var deltatBenchStr = "ΔF: Gathering data"

    fun draw(frameDelta: Float, projMat: Matrix4, fbo: FrameBuffer): FrameBuffer {

        val showTimepieceOption = App.getConfigString("show_timepiece_overlay")
        val showTimepiece = when (showTimepieceOption) {
            "hide" -> false
            "always" -> true
            else -> App.scr.isFullscreen
        }

        // init
        if (!init) {
            init = true

            debugUI.setPosition(0, 0)

            batch = FlippingSpriteBatch()
            camera = OrthographicCamera(App.scr.wf, App.scr.hf)
            camera.setToOrtho(true)

            batch.projectionMatrix = camera.combined

            shapeRenderer = App.makeShapeRenderer()
            shapeRenderer.projectionMatrix = camera.combined

            Gdx.gl20.glViewport(0, 0, App.scr.width, App.scr.height)

            resize(App.scr.width, App.scr.height)

            update()
        }

        // update every 1 second or so
        if (App.GLOBAL_RENDER_TIMER % Gdx.graphics.framesPerSecond.coerceAtLeast(20) == 1L)
            update()


        debugUI.update(Gdx.graphics.deltaTime)

        outFBO.inAction(camera, batch) {
            App.measureDebugTime("Renderer.PostProcessor") {

                gdxClearAndEnableBlend(0f, 1f, 0f, 1f)

                fbo.colorBufferTexture.setFilter(
                        Texture.TextureFilter.Linear,
                        if (App.scr.magn % 1.0 < 0.0001) Texture.TextureFilter.Nearest else Texture.TextureFilter.Linear
                )

                drawFBOwithDither(projMat, fbo)

                // draw things when F keys are on
                if (App.IS_DEVELOPMENT_BUILD && KeyToggler.isOn(Input.Keys.F11)) {
                    drawSafeArea()
                }


                if (KeyToggler.isOn(Input.Keys.F1)) {
                    batch.color = Color.WHITE
                    batch.inUse {
                        it.draw(functionRowHelper,
                                (App.scr.width - functionRowHelper.width) / 2f,
                                0f,
                                functionRowHelper.width.toFloat(),
                                functionRowHelper.height.toFloat()
                        )
                    }
                }

                if (KeyToggler.isOn(Input.Keys.F10)) {
                    batch.color = Color.WHITE
                    batch.inUse {
                        // print wire draw class
                        App.fontSmallNumbers.draw(it, "${ccY}Wire draw class: $ccG${(Terrarum.ingame as? net.torvald.terrarum.modulebasegame.TerrarumIngame)?.selectedWireRenderClass}", 2f, 2f)

                        // print UIs under cursor
                        App.fontSmallNumbers.draw(it, "${ccY}UIs under mouse:", 2f, 15f)
                        Terrarum.ingame?.uiContainer?.UIsUnderMouse?.forEachIndexed { i, ui ->
                            App.fontSmallNumbers.draw(it, "${ccY}-$ccG ${ui.javaClass.simpleName} (0x${UnsafeHelper.addressOf(ui).toHex()})", 2f, 28f + 13*i)
                        }
                    }
                }

                if (KeyToggler.isOn(Input.Keys.F3)) {
                    if (!debugUI.isOpened && !debugUI.isOpening) debugUI.setAsOpen()
                    batch.inUse { debugUI.renderImpl(frameDelta, batch, camera) }
                }
                else {
                    if (!debugUI.isClosed && !debugUI.isClosing) debugUI.setAsClose()
                }

                if (showTimepiece) {
                    drawFullscreenComplications()
                }

                // draw dev build notifiers
                // omitting this screws up HQ2X render for some reason
                if (Terrarum.ingame != null) {
                    batch.inUse {
                        batch.color = if (IS_DEVELOPMENT_BUILD) safeAreaCol else colourNull
                        App.fontGame.draw(it, thisIsDebugStr, 5f, App.scr.height - 24f)
                    }
                }

                if (KeyToggler.isOn(Input.Keys.F2)) {
                    if (INGAME.WORLD_UPDATE_TIMER % 64 == 42L) {
                        // we're going to assume the data are normally distributed
                        deltatBenchStr = if (INGAME.deltaTeeBenchmarks.elemCount < INGAME.deltaTeeBenchmarks.size) {
                            "ΔF: Gathering data (${INGAME.deltaTeeBenchmarks.elemCount}/${INGAME.deltaTeeBenchmarks.size})"
                        }
                        else {
                            val tallies = INGAME.deltaTeeBenchmarks.toList().sorted()
                            val average = tallies.average()

                            val halfPos = 0.5f * INGAME.deltaTeeBenchmarks.size
                            val halfInd = halfPos.floorToInt()
                            val low5pos = 0.05f * INGAME.deltaTeeBenchmarks.size
                            val low5ind = low5pos.floorToInt()
                            val low1pos = 0.01f * INGAME.deltaTeeBenchmarks.size
                            val low1ind = low1pos.floorToInt()

                            val median = FastMath.interpolateLinear(halfPos - halfInd, tallies[halfInd], tallies[halfInd + 1])
                            val low5 = FastMath.interpolateLinear(low5pos - low5ind, tallies[low5ind], tallies[low5ind + 1])
                            val low1 = FastMath.interpolateLinear(low1pos - low1ind, tallies[low1ind], tallies[low1ind + 1])
                            "ΔF: Avr ${average.format(1)}; Med ${median.format(1)}; 5% ${low5.format(1)}; 1% ${low1.format(1)}"
                        }
                    }
                    batch.color = Toolkit.Theme.COL_MOUSE_UP
                    batch.inUse {
                        val tw = App.fontGame.getWidth(deltatBenchStr)
                        App.fontGame.draw(it, deltatBenchStr, Toolkit.drawWidth - tw - 5f, App.scr.height - 24f)
                    }
                }
            }
        }

        return outFBO
    }
    private val rng = HQRNG()
    private val colourNull = Color(0)

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    private val swizzler = floatArrayOf(
        1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f,
        1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,0f,1f, 0f,0f,1f,0f,
        1f,0f,0f,0f, 0f,0f,1f,0f, 0f,1f,0f,0f, 0f,0f,0f,1f,
        1f,0f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f, 0f,1f,0f,0f,
        1f,0f,0f,0f, 0f,0f,0f,1f, 0f,1f,0f,0f, 0f,0f,1f,0f,
        1f,0f,0f,0f, 0f,0f,0f,1f, 0f,0f,1f,0f, 0f,1f,0f,0f,

        0f,1f,0f,0f, 1f,0f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f,
        0f,1f,0f,0f, 1f,0f,0f,0f, 0f,0f,0f,1f, 0f,0f,1f,0f,
        0f,1f,0f,0f, 0f,0f,1f,0f, 1f,0f,0f,0f, 0f,0f,0f,1f,
        0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f, 1f,0f,0f,0f,
        0f,1f,0f,0f, 0f,0f,0f,1f, 1f,0f,0f,0f, 0f,0f,1f,0f,
        0f,1f,0f,0f, 0f,0f,0f,1f, 0f,0f,1f,0f, 1f,0f,0f,0f,

        0f,0f,1f,0f, 1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,0f,1f,
        0f,0f,1f,0f, 1f,0f,0f,0f, 0f,0f,0f,1f, 0f,1f,0f,0f,
        0f,0f,1f,0f, 0f,1f,0f,0f, 1f,0f,0f,0f, 0f,0f,0f,1f,
        0f,0f,1f,0f, 0f,1f,0f,0f, 0f,0f,0f,1f, 1f,0f,0f,0f,
        0f,0f,1f,0f, 0f,0f,0f,1f, 1f,0f,0f,0f, 0f,1f,0f,0f,
        0f,0f,1f,0f, 0f,0f,0f,1f, 0f,1f,0f,0f, 1f,0f,0f,0f,

        0f,0f,0f,1f, 1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f,
        0f,0f,0f,1f, 1f,0f,0f,0f, 0f,0f,1f,0f, 0f,1f,0f,0f,
        0f,0f,0f,1f, 0f,1f,0f,0f, 1f,0f,0f,0f, 0f,0f,1f,0f,
        0f,0f,0f,1f, 0f,1f,0f,0f, 0f,0f,1f,0f, 1f,0f,0f,0f,
        0f,0f,0f,1f, 0f,0f,1f,0f, 1f,0f,0f,0f, 0f,1f,0f,0f,
        0f,0f,0f,1f, 0f,0f,1f,0f, 0f,1f,0f,0f, 1f,0f,0f,0f,
    )

    private fun drawFBOwithDither(projMat: Matrix4, fbo: FrameBuffer) {

        val shader = if (App.getConfigBoolean("fx_dither"))
            shaderPostDither
        else
            shaderPostNoDither

        App.getCurrentDitherTex().bind(1)
        fbo.colorBufferTexture.bind(0)
//        testfill.bind(0)

        shader.bind()
        shader.setUniformMatrix("u_projTrans", projMat)
        shader.setUniformi("u_texture", 0)
        shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
        shader.setUniformi("u_pattern", 1)
        shader.setUniformf("quant", shaderQuant[App.getConfigInt("displaycolourdepth")] ?: 255f)
//        shader.setUniformf("quant", 1f)
        shader.setUniformMatrix4fv("swizzler", swizzler, rng.nextInt(24), 16*4)
        App.fullscreenQuad.render(shader, GL20.GL_TRIANGLE_FAN)

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

    }

    private fun drawSafeArea() {
        val magn = App.scr.magn

        val tvSafeAreaW = App.scr.tvSafeGraphicsWidth.toFloat()
        val tvSafeAreaH = App.scr.tvSafeGraphicsHeight.toFloat()
        val tvSafeArea2W = App.scr.tvSafeActionWidth.toFloat()
        val tvSafeArea2H = App.scr.tvSafeActionHeight.toFloat()
        val uiAreaHeight = App.scr.height - 2 * tvSafeAreaH
        val uiAreaWidth = uiAreaHeight * recommendRatio

        val scrw = Toolkit.drawWidthf * magn
        val scrh = App.scr.hf * magn

        val rect2W = tvSafeArea2W * magn
        val rect2H = tvSafeArea2H * magn
        val rectW = tvSafeAreaW * magn
        val rectH = tvSafeAreaH * magn

        val macbookNotchRatio = 35f / 290f
        val macbookNotchWidth = scrw * macbookNotchRatio
        val macbookNotchHeight = rect2H

        shapeRenderer.inUse(ShapeRenderer.ShapeType.Line) {

            // centre ind
            shapeRenderer.color = safeAreaCol2
            shapeRenderer.line(0f, 0f, scrw, scrh)
            shapeRenderer.line(0f, scrh, scrw, 0f)

            // macos notch border
            shapeRenderer.color = noGoAreaCol
            shapeRenderer.rect(
                (scrw - macbookNotchWidth) / 2f,
                0f,
                macbookNotchWidth,
                macbookNotchHeight,
            )

            for (k in 0 until 8) {
                val y1 = macbookNotchHeight * (k / 8f)
                val y2 = y1
                val x1 = (scrw - macbookNotchWidth) / 2f
                val x2 = (scrw - macbookNotchWidth) / 2f + macbookNotchWidth
                shapeRenderer.line(x1, y1, x2, y2)
            }

            // safe action area
            shapeRenderer.color = safeAreaCol2
            shapeRenderer.rect(
                    rect2W, rect2H, scrw - 2 * rect2W, App.scr.height * magn - 2 * rect2H
            )

            // safe graphics area
            shapeRenderer.color = safeAreaCol
            shapeRenderer.rect(
                    rectW, rectH, scrw - 2 * rectW, App.scr.height * magn - 2 * rectH
            )

            // default res ind
            shapeRenderer.color = defaultResCol
            shapeRenderer.rect(
                    (scrw - uiAreaWidth * magn).div(2f),
                    (App.scr.height - uiAreaHeight).times(magn).div(2f),
                    uiAreaWidth * magn,
                    uiAreaHeight * magn
            )
        }

        try {
            batch.inUse {
                batch.color = safeAreaCol
                App.fontSmallNumbers.draw(
                        batch, safeAreaStr,
                        tvSafeAreaW, tvSafeAreaH - 12
                )

                batch.color = defaultResCol
                App.fontSmallNumbers.draw(
                        batch, defaultResStr,
                        (Toolkit.drawWidthf - uiAreaWidth).div(2f),
                        tvSafeAreaH
                )

                batch.color = currentResCol
                App.fontSmallNumbers.draw(
                        batch, currentResStr,
                        Toolkit.drawWidthf - 80f,
                        0f
                )
            }
        }
        catch (doNothing: NullPointerException) { }
        finally {
            // one-time call, caused by catching NPE before batch ends
            if (batch.isDrawing) {
                batch.end()
            }
        }
    }

    fun update() {
        val time = ZonedDateTime.now()
        clockH = time.hour.toString().padStart(2,'0')
        clockM = time.minute.toString().padStart(2,'0')

        val battStatus = GetBatteryStatus.get()

        hasBattery = battStatus.hasBattery
        isCharging = battStatus.isCharging
        batteryPercentage = battStatus.percentage

        val timeNow = App.getTIME_T()
        val ptime_t = timeNow - App.loadedTime_t

        val ptimeMin = ptime_t / 60
        val ptimeHr = ptime_t / 3600

        ptimeH = ptimeHr.toString().padStart(2,'0')
        ptimeM = ptimeMin.toString().padStart(2,'0')
    }

    private var clockH = "00"
    private var clockM = "00"
    private var hasBattery = false
    private var isCharging = false
    private var batteryPercentage = 0

    private var ptimeH = "00"
    private var ptimeM = "00"

    private val shadowCol = Color(1f, 1f, 1f, 0.6666667f)

    private fun drawFullscreenComplications() {
        val tvSafeArea2H = App.scr.tvSafeActionHeight.toFloat()
        val dockHeight = tvSafeArea2H
        val watchWidth = App.fontSmallNumbers.W * 5
        val watchHeight = 14
        val marginEach = (dockHeight - watchHeight) / 2f
        val wx = (App.scr.width - marginEach * 1.5f - watchWidth).floorToFloat()
        val wy = marginEach.ceilToFloat()
        val watchStr = "$clockH:$clockM"
        val batteryPercentageStr = "$batteryPercentage%"

        val ptimex = (marginEach * 1.5f).ceilToFloat()
        val ptimey = wy
        val ptimew = 7*6 + 4f
        val ptimestr = "$ptimeH:$ptimeM"

        val percIndex = (batteryPercentage.toFloat() * 0.01 * 63).toInt() // 0-63
        val btx = percIndex % 4
        val bty = percIndex / 4
        val btxoff = isCharging.toInt() * 4

        batch.inUse {

            // draw blur backs
            batch.color = shadowCol
            Toolkit.drawBlurShadowBack(batch, wx, wy + 2, App.fontSmallNumbers.getWidth(watchStr).toFloat(), 9f)

            if (hasBattery) {
                Toolkit.drawBlurShadowBack(batch, wx - watchHeight - batteryTex.tileW - App.fontSmallNumbers.getWidth(batteryPercentageStr) - 4, wy + 2, App.fontSmallNumbers.getWidth(batteryPercentageStr) + 28f, 9f)
            }

            Toolkit.drawBlurShadowBack(batch, ptimex, ptimey + 2, ptimew, 9f)



            // draw texts
            batch.color = Color.WHITE
            App.fontSmallNumbers.draw(batch, watchStr, wx, wy)

            if (hasBattery) {
                val batCell = batteryTex.get(btxoff + btx, bty)
                batch.draw(batCell, wx - watchHeight - batCell.regionWidth, wy)

                App.fontSmallNumbers.draw(
                    batch, batteryPercentageStr,
                    wx - watchHeight - batCell.regionWidth - App.fontSmallNumbers.getWidth(batteryPercentageStr) - 4,
                    wy
                )
            }

            App.fontSmallNumbers.draw(batch, "\u00DD", ptimex, ptimey)
            App.fontSmallNumbers.draw(batch, ptimestr, ptimex + 11, ptimey)

        }
    }

    private val defaultResStr = "Ingame UI Area"
    private val currentResStr = "${App.scr.width}x${App.scr.height}"
    private val safeAreaStr = "TV Safe Area"
    private val versionStr = "${App.getVERSION_STRING()}"
    internal val thisIsDebugStr = "${App.GAME_NAME} ${if (App.IS_DEVELOPMENT_BUILD) "Development Build" else "Release"} $versionStr"

    /**
     * Camera will be moved so that (newX, newY) would be sit on the top-left edge.
     */
    /*private fun setCameraPosition(newX: Float, newY: Float) {
        camera.position.set((-newX + AppLoader.halfScreenW).round(), (-newY + AppLoader.halfScreenH).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }*/

}