package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.worlddrawer.BlocksDrawer

/**
 * Must be called by the App Loader
 */
object PostProcessor : Disposable {

    private lateinit var batch: SpriteBatch // not nulling to save some lines of code
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera

    private lateinit var lutTex: Texture

    private var init = false

    fun reloadLUT(filename: String) {
        lutTex = Texture(Gdx.files.internal("assets/clut/$filename"))
    }

    private val defaultResCol = Color(0x66ffff66)
    private val safeAreaCol = Color(0xffffff66.toInt())
    private val safeAreaCol2 = Color(0xffffff44.toInt())
    private val currentResCol = Color(0xffffee44.toInt())

    private val debugUI = BasicDebugInfoWindow()

    private val functionRowHelper = Texture(Gdx.files.internal("assets/graphics/function_row_help.png"))

    init {
        AppLoader.disposableSingletonsPool.add(this)
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        functionRowHelper.dispose()
        try {
            lutTex.dispose()
        }
        catch (e: UninitializedPropertyAccessException) { }
    }

    fun draw(projMat: Matrix4, fbo: FrameBuffer) {

        // init
        if (!init) {
            init = true

            debugUI.setPosition(0, 0)

            batch = SpriteBatch()
            camera = OrthographicCamera(AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)
            camera.setToOrtho(true)

            batch.projectionMatrix = camera.combined

            shapeRenderer = ShapeRenderer()
            Gdx.gl20.glViewport(0, 0, AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)
        }


        debugUI.update(Gdx.graphics.deltaTime)


        AppLoader.measureDebugTime("Renderer.PostProcessor") {

            gdxClearAndSetBlend(.094f, .094f, .094f, 0f)

            postShader(projMat, fbo)

            // draw things when F keys are on
            if (AppLoader.IS_DEVELOPMENT_BUILD && KeyToggler.isOn(Input.Keys.F11)) {
                drawSafeArea()
            }

            if (KeyToggler.isOn(Input.Keys.F1)) {
                batch.color = Color.WHITE
                batch.inUse {
                    it.draw(functionRowHelper,
                            (AppLoader.screenSize.screenW - functionRowHelper.width) / 2f,
                            functionRowHelper.height.toFloat(),
                            functionRowHelper.width.toFloat(),
                            functionRowHelper.height * -1f
                    )
                }
            }

            if (KeyToggler.isOn(Input.Keys.F10)) {
                batch.color = Color.WHITE
                batch.inUse {
                    AppLoader.fontSmallNumbers.draw(it, "Wire draw class: ${(Terrarum.ingame as? net.torvald.terrarum.modulebasegame.TerrarumIngame)?.selectedWireRenderClass}", 2f, 2f)
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
            if (AppLoader.IS_DEVELOPMENT_BUILD && Terrarum.ingame != null) {
                batch.inUse {
                    batch.color = safeAreaCol
                    AppLoader.fontGame.draw(it, thisIsDebugStr, 5f, AppLoader.screenSize.screenH - 24f)
                }
            }
        }
    }

    private fun postShader(projMat: Matrix4, fbo: FrameBuffer) {
        val shader: ShaderProgram? =
                if (AppLoader.getConfigBoolean("fxretro"))
                    AppLoader.shaderHicolour
                else
                    AppLoader.shaderPassthruRGB

        fbo.colorBufferTexture.bind(0)

        shader?.bind()
        shader?.setUniformMatrix("u_projTrans", projMat)
        shader?.setUniformi("u_texture", 0)
        AppLoader.fullscreenQuad.render(shader, GL20.GL_TRIANGLES)


        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

    }

    private fun drawSafeArea() {
        val tvSafeAreaW = AppLoader.screenSize.tvSafeGraphicsWidth.toFloat()
        val tvSafeAreaH = AppLoader.screenSize.tvSafeGraphicsHeight.toFloat()
        val tvSafeArea2W = AppLoader.screenSize.tvSafeActionWidth.toFloat()
        val tvSafeArea2H = AppLoader.screenSize.tvSafeActionHeight.toFloat()

        shapeRenderer.inUse(ShapeRenderer.ShapeType.Line) {

            // centre ind
            shapeRenderer.color = safeAreaCol2
            shapeRenderer.line(0f, 0f, AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)
            shapeRenderer.line(0f, AppLoader.screenSize.screenHf, AppLoader.screenSize.screenWf, 0f)

            // safe action area
            shapeRenderer.color = safeAreaCol2
            shapeRenderer.rect(
                    tvSafeArea2W, tvSafeArea2H, AppLoader.screenSize.screenW - 2 * tvSafeArea2W, AppLoader.screenSize.screenH - 2 * tvSafeArea2H
            )

            // safe graphics area
            shapeRenderer.color = safeAreaCol
            shapeRenderer.rect(
                    tvSafeAreaW, tvSafeAreaH, AppLoader.screenSize.screenW - 2 * tvSafeAreaW, AppLoader.screenSize.screenH - 2 * tvSafeAreaH
            )

            // default res ind
            shapeRenderer.color = defaultResCol
            shapeRenderer.rect(
                    (AppLoader.screenSize.screenW - TerrarumScreenSize.minimumW).div(2).toFloat(),
                    (AppLoader.screenSize.screenH - TerrarumScreenSize.minimumH).div(2).toFloat(),
                    TerrarumScreenSize.minimumW.toFloat(),
                    TerrarumScreenSize.minimumH.toFloat()
            )
        }

        try {
            batch.inUse {
                batch.color = safeAreaCol
                AppLoader.fontSmallNumbers.draw(
                        batch, safeAreaStr,
                        tvSafeAreaW, tvSafeAreaH - 10
                )

                batch.color = defaultResCol
                AppLoader.fontSmallNumbers.draw(
                        batch, defaultResStr,
                        (AppLoader.screenSize.screenW - TerrarumScreenSize.minimumW).div(2).toFloat(),
                        (AppLoader.screenSize.screenH - TerrarumScreenSize.minimumH).div(2).toFloat()
                )

                batch.color = currentResCol
                AppLoader.fontSmallNumbers.draw(
                        batch, currentResStr,
                        AppLoader.screenSize.screenW - 80f,
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

    private val defaultResStr = "${TerrarumScreenSize.minimumW}x${TerrarumScreenSize.minimumH}"
    private val currentResStr = "${AppLoader.screenSize.screenW}x${AppLoader.screenSize.screenH}"
    private val safeAreaStr = "TV Safe Area"
    private val versionStr = "Version ${AppLoader.getVERSION_STRING()}"
    private val thisIsDebugStr = "${AppLoader.GAME_NAME} Develoment Build $versionStr"

    /**
     * Camera will be moved so that (newX, newY) would be sit on the top-left edge.
     */
    /*private fun setCameraPosition(newX: Float, newY: Float) {
        camera.position.set((-newX + AppLoader.halfScreenW).round(), (-newY + AppLoader.halfScreenH).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }*/

}