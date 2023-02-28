package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.Toolkit

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

    private val debugUI = BasicDebugInfoWindow()

    private val functionRowHelper = Texture(Gdx.files.internal("assets/graphics/function_row_help.png"))


    private val shaderPostDither = App.loadShaderFromClasspath("shaders/default.vert", "shaders/postproc_dither.frag")
    private val shaderPostNoDither = App.loadShaderFromClasspath("shaders/default.vert", "shaders/postproc_nodither.frag")

    private val recommendRatio = 1.5f

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
        try { outFBO.dispose() } catch (_: UninitializedPropertyAccessException) {}
        outFBO = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        functionRowHelper.dispose()
        try { lutTex.dispose() } catch (_: UninitializedPropertyAccessException) {}
        shaderPostDither.dispose()
        shaderPostNoDither.dispose()
        outFBO.dispose()
    }

    private var deltatBenchStr = "ΔF: Gathering data"

    fun draw(projMat: Matrix4, fbo: FrameBuffer): FrameBuffer {

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
        }


        debugUI.update(Gdx.graphics.deltaTime)

        outFBO.inAction(camera, batch) {
            App.measureDebugTime("Renderer.PostProcessor") {

                gdxClearAndEnableBlend(.094f, .094f, .094f, 0f)

                fbo.colorBufferTexture.setFilter(
                        Texture.TextureFilter.Linear,
                        if (App.scr.magn % 1.0 < 0.0001) Texture.TextureFilter.Nearest else Texture.TextureFilter.Linear
                )

                postShader(projMat, fbo)

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
                        App.fontSmallNumbers.draw(it, "Wire draw class: ${(Terrarum.ingame as? net.torvald.terrarum.modulebasegame.TerrarumIngame)?.selectedWireRenderClass}", 2f, 2f)
                    }
                }

                if (KeyToggler.isOn(Input.Keys.F3)) {
                    if (!debugUI.isOpened && !debugUI.isOpening) debugUI.setAsOpen()
                    batch.inUse { debugUI.renderUI(batch, camera) }
                }
                else {
                    if (!debugUI.isClosed && !debugUI.isClosing) debugUI.setAsClose()
                }

                // draw dev build notifiers
                if (App.IS_DEVELOPMENT_BUILD && Terrarum.ingame != null) {
                    batch.inUse {
                        batch.color = safeAreaCol
                        App.fontGame.draw(it, thisIsDebugStr, 5f, App.scr.height - 24f)
                    }
                }

                if (KeyToggler.isOn(App.getConfigInt("debug_key_deltat_benchmark"))) {
                    if (INGAME.WORLD_UPDATE_TIMER % 64 == 42) {
                        // we're going to assume the data are normally distributed
                        deltatBenchStr = if (INGAME.deltaTeeBenchmarks.elemCount < INGAME.deltaTeeBenchmarks.size) {
                            "ΔF: Gathering data (${INGAME.deltaTeeBenchmarks.elemCount}/${INGAME.deltaTeeBenchmarks.size})"
                        }
                        else {
                            val tallies = INGAME.deltaTeeBenchmarks.toList().sorted()
                            val average = tallies.average()

                            val halfPos = 0.5f * INGAME.deltaTeeBenchmarks.size
                            val halfInd = halfPos.floorInt()
                            val low5pos = 0.05f * INGAME.deltaTeeBenchmarks.size
                            val low5ind = low5pos.floorInt()
                            val low1pos = 0.01f * INGAME.deltaTeeBenchmarks.size
                            val low1ind = low1pos.floorInt()

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

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    private val swizzler = intArrayOf(
            1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1,
            1,0,0,0, 0,1,0,0, 0,0,0,1, 0,0,1,0,
            1,0,0,0, 0,0,1,0, 0,1,0,0, 0,0,0,1,
            1,0,0,0, 0,0,1,0, 0,0,0,1, 0,1,0,0,
            1,0,0,0, 0,0,0,1, 0,1,0,0, 0,0,1,0,
            1,0,0,0, 0,0,0,1, 0,0,1,0, 0,1,0,0,

            0,1,0,0, 1,0,0,0, 0,0,1,0, 0,0,0,1,
            0,1,0,0, 1,0,0,0, 0,0,0,1, 0,0,1,0,
            0,1,0,0, 0,0,1,0, 1,0,0,0, 0,0,0,1,
            0,1,0,0, 0,0,1,0, 0,0,0,1, 1,0,0,0,
            0,1,0,0, 0,0,0,1, 1,0,0,0, 0,0,1,0,
            0,1,0,0, 0,0,0,1, 0,0,1,0, 1,0,0,0,

            0,0,1,0, 1,0,0,0, 0,1,0,0, 0,0,0,1,
            0,0,1,0, 1,0,0,0, 0,0,0,1, 0,1,0,0,
            0,0,1,0, 0,1,0,0, 1,0,0,0, 0,0,0,1,
            0,0,1,0, 0,1,0,0, 0,0,0,1, 1,0,0,0,
            0,0,1,0, 0,0,0,1, 1,0,0,0, 0,1,0,0,
            0,0,1,0, 0,0,0,1, 0,1,0,0, 1,0,0,0,

            0,0,0,1, 1,0,0,0, 0,1,0,0, 0,0,1,0,
            0,0,0,1, 1,0,0,0, 0,0,1,0, 0,1,0,0,
            0,0,0,1, 0,1,0,0, 1,0,0,0, 0,0,1,0,
            0,0,0,1, 0,1,0,0, 0,0,1,0, 1,0,0,0,
            0,0,0,1, 0,0,1,0, 1,0,0,0, 0,1,0,0,
            0,0,0,1, 0,0,1,0, 0,1,0,0, 1,0,0,0,
    ).map { it.toFloat() }.toFloatArray()

    private fun postShader(projMat: Matrix4, fbo: FrameBuffer) {

        val shader = if (App.getConfigBoolean("fx_dither"))
            shaderPostDither
        else
            shaderPostNoDither

        App.getCurrentDitherTex().bind(1)
        fbo.colorBufferTexture.bind(0)

        shader.bind()
        shader.setUniformMatrix("u_projTrans", projMat)
        shader.setUniformi("u_texture", 0)
        shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
        shader.setUniformi("u_pattern", 1)
        shader.setUniformf("quant", shaderQuant[App.getConfigInt("displaycolourdepth")] ?: 255f)
        shader.setUniformMatrix4fv("swizzler", swizzler, rng.nextInt(24), 16*4)
        App.fullscreenQuad.render(shader, GL20.GL_TRIANGLES)

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

        shapeRenderer.inUse(ShapeRenderer.ShapeType.Line) {

            // centre ind
            shapeRenderer.color = safeAreaCol2
            shapeRenderer.line(0f, 0f, scrw, scrh)
            shapeRenderer.line(0f, scrh, scrw, 0f)

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

    private val defaultResStr = "Ingame UI Area"
    private val currentResStr = "${App.scr.width}x${App.scr.height}"
    private val safeAreaStr = "TV Safe Area"
    private val versionStr = "Version ${App.getVERSION_STRING()}"
    internal val thisIsDebugStr = "${App.GAME_NAME} Development Build $versionStr"

    /**
     * Camera will be moved so that (newX, newY) would be sit on the top-left edge.
     */
    /*private fun setCameraPosition(newX: Float, newY: Float) {
        camera.position.set((-newX + AppLoader.halfScreenW).round(), (-newY + AppLoader.halfScreenH).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }*/

}