package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.Color
import net.torvald.unicode.EMDASH
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap


/**
 * Created by minjaesong on 2017-06-11.
 */
class TestTestTest(val batch: SpriteBatch) : Screen {

    lateinit var img: Texture

    lateinit var gameFont: BitmapFont

    lateinit var blurFboA: FrameBuffer
    lateinit var blurFboB: FrameBuffer

    lateinit var worldFbo: FrameBuffer

    lateinit var camera: OrthographicCamera

    // invert Y
    fun initViewPort(width: Int, height: Int) {
        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }

    fun enter() {
        // init view port
        camera = OrthographicCamera(App.scr.wf, App.scr.hf)


        img = Texture("assets/test_texture.tga")

        gameFont = TerrarumSansBitmap("assets/graphics/fonts/terrarum-sans-bitmap")
        //gameFont = BitmapFont()


        blurFboA = FrameBuffer(Pixmap.Format.RGBA8888, img.width, img.height, false)
        blurFboB = FrameBuffer(Pixmap.Format.RGBA8888, img.width, img.height, false)

        worldFbo = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, false)

        //blurShader.begin()
        //blurShader.setUniformf("iResolution", img.width.toFloat(), img.height.toFloat(), 0f)
        //blurShader.end()


        initViewPort(App.scr.width, App.scr.height)
    }

    override fun render(delta: Float) {
        Gdx.graphics.setTitle("TestTestTest $EMDASH F: ${Gdx.graphics.framesPerSecond}")


        val iterations = 16 // ideally, 4 * radius; must be even number -- odd number will flip the image
        val radius = 4f


        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        blurFboA.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        blurFboB.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }


        var writeBuffer = blurFboA
        var readBuffer = blurFboB


        for (i in 0..iterations - 1) {
            writeBuffer.inAction(camera, batch) {

                batch.inUse {
                    val texture = if (i == 0)
                        img
                    else
                        readBuffer.colorBufferTexture

                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)


                    batch.shader = TestTestMain.blurShader
                    batch.shader.setUniformf("iResolution", writeBuffer.width.toFloat(), writeBuffer.height.toFloat())
                    batch.shader.setUniformf("flip", 1f)
                    if (i % 2 == 0)
                        batch.shader.setUniformf("direction", radius, 0f)
                    else
                        batch.shader.setUniformf("direction", 0f, radius)


                    batch.color = Color.WHITE
                    batch.draw(texture, 0f, 0f)


                    // swap
                    val t = writeBuffer
                    writeBuffer = readBuffer
                    readBuffer = t
                }
            }
        }


        worldFbo.inAction(camera, batch) {
            Gdx.gl.glClearColor(0f,0f,0f,0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            batch.inUse {
                batch.shader = null

                camera.position.set(App.scr.width / 2f - 50f, App.scr.height / 2f - 50f, 0f)
                camera.update()
                batch.projectionMatrix = camera.combined



                batch.color = Color.WHITE
                batch.draw(writeBuffer.colorBufferTexture, 0f, 0f)
            }
        }


        camera.setToOrtho(true, App.scr.wf, App.scr.hf)
        batch.projectionMatrix = camera.combined
        batch.inUse {

            camera.position.set(App.scr.width / 2f, App.scr.height / 2f, 0f)
            camera.update()
            batch.projectionMatrix = camera.combined


            batch.color = Color.WHITE
            batch.draw(worldFbo.colorBufferTexture, 0f, 0f)


            batch.draw(img, 0f, 0f, 100f, 100f)
        }
    }

    override fun hide() {
    }

    override fun show() {
        initViewPort(App.scr.width, App.scr.height)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }


}

object TestTestMain : ApplicationAdapter() {
    lateinit var blurShader: ShaderProgram
    lateinit var batch: SpriteBatch

    lateinit var currentScreen: TestTestTest

    override fun create() {
        ShaderProgram.pedantic = false
        blurShader = ShaderProgram(Gdx.files.internal("assets/shaders/blur.vert"), Gdx.files.internal("assets/shaders/blur.frag"))


        batch = SpriteBatch()


        currentScreen = TestTestTest(batch)
        currentScreen.enter()
    }

    override fun render() {
        currentScreen.render(Gdx.graphics.deltaTime)
    }

    override fun dispose() {
        currentScreen.dispose()
    }
}

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = Lwjgl3ApplicationConfiguration()
    config.useVsync(false)
    config.setResizable(false)
    config.setWindowedMode(1072, 742)
    Lwjgl3Application(TestTestMain, config)
}