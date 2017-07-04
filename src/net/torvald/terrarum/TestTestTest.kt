package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import com.badlogic.gdx.graphics.OrthographicCamera




/**
 * Created by minjaesong on 2017-06-11.
 */
class TestTestTest : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var img: Texture

    lateinit var gameFont: BitmapFont

    lateinit var blurShader: ShaderProgram

    lateinit var blurFboA: FrameBuffer
    lateinit var blurFboB: FrameBuffer

    lateinit var cam: OrthographicCamera

    override fun create() {
        batch = SpriteBatch()
        img = Texture("assets/test_texture.tga")

        gameFont = GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap")
        //gameFont = BitmapFont()


        blurFboA = FrameBuffer(Pixmap.Format.RGBA8888, img.width, img.height, false)
        blurFboB = FrameBuffer(Pixmap.Format.RGBA8888, img.width, img.height, false)

        ShaderProgram.pedantic = false
        blurShader = ShaderProgram(Gdx.files.internal("assets/blur.vert"), Gdx.files.internal("assets/blur.frag"))

        blurShader.begin()
        blurShader.setUniformf("iResolution", img.width.toFloat(), img.height.toFloat(), 0f)
        blurShader.end()


        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.setToOrtho(false)
    }

    override fun render() {
        val iterations = 16
        val radius = 4f


        Gdx.gl.glClearColor(.157f, .157f, .157f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        blurFboA.inAction {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        blurFboB.inAction {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }


        var writeBuffer = blurFboA
        var readBuffer = blurFboB


        for (i in 0..iterations - 1) {
            writeBuffer.inAction {
                batch.inUse {
                    cam.setToOrtho(false, writeBuffer.width.toFloat(), writeBuffer.height.toFloat())
                    batch.projectionMatrix = cam.combined


                    val texture = if (i == 0)
                        img
                    else
                        readBuffer.colorBufferTexture

                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

                    batch.shader = blurShader
                    batch.shader.setUniformf("flip", 1f)
                    if (i % 2 == 0)
                        batch.shader.setUniformf("direction", radius, 0f)
                    else
                        batch.shader.setUniformf("direction", 0f, radius)



                    batch.draw(texture, 0f, 0f)


                    // swap
                    val t = writeBuffer
                    writeBuffer = readBuffer
                    readBuffer = t
                }
            }
        }

        // draw last FBO to screen
        batch.inUse {
            cam.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            batch.projectionMatrix = cam.combined


            batch.shader.setUniformf("direction", 0f, 0f)
            batch.shader.setUniformf("flip", if (iterations % 2 != 0) 1f else 0f)
            batch.draw(writeBuffer.colorBufferTexture, 0f, 0f)
        }
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }


    private inline fun SpriteBatch.inUse(action: () -> Unit) {
        this.begin()
        action()
        this.end()
    }

    inline fun FrameBuffer.inAction(action: (FrameBuffer) -> Unit) {
        this.begin()
        action(this)
        this.end()
    }

}

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    //config.useGL30 = true
    config.vSyncEnabled = false
    config.resizable = false
    config.width = 1072
    config.height = 742
    config.foregroundFPS = 9999
    LwjglApplication(TestTestTest(), config)
}