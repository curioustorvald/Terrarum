package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.unicode.EMDASH

/**
 * Created by minjaesong on 2017-07-05.
 */

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = Lwjgl3ApplicationConfiguration()
    config.useVsync(false)
    config.setResizable(false)
    config.setWindowedMode(1072, 742)
    Lwjgl3Application(ColorLimiterTest, config)
}

object ColorLimiterTest : ApplicationAdapter() {

    lateinit var img: Texture
    lateinit var shader4096: ShaderProgram

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer

    lateinit var font: TerrarumSansBitmap

    override fun create() {
        ShaderProgram.pedantic = false

        shader4096 = ShaderProgram(Gdx.files.internal("assets/shaders/default.vert"), Gdx.files.internal("assets/shaders/postproc_dither.frag"))
        shader4096.bind()
        shader4096.setUniformf("rcount", 4f)
        shader4096.setUniformf("gcount", 4f)
        shader4096.setUniformf("bcount", 4f)

        //img = Texture("assets/test_gradient.tga")
        img = Texture("assets/test_texture.tga")

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        font = TerrarumSansBitmap("assets/graphics/fonts/terrarum-sans-bitmap")


        if (!shader4096.isCompiled) {
            Gdx.app.log("Shader", shader4096.log)
            System.exit(1)
        }
    }

    private var timer = 0f
    private var timerTick = 1f
    private var ditherStart = 2f
    private var ditherEnd = 16f
    private var dither = ditherStart

    override fun render() {
        timer += Gdx.graphics.deltaTime

        if (timer > timerTick) {
            timer -= timerTick
            dither += 1f
            if (dither > ditherEnd)
                dither = ditherStart
        }

        Gdx.graphics.setTitle("TestTestTest $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        batch.inUse {
            batch.shader = shader4096
            shader4096.setUniformf("rcount", 6f)//dither)
            shader4096.setUniformf("gcount", 6f)//dither)
            shader4096.setUniformf("bcount", 6f)//dither)
            batch.color = Color.WHITE
            batch.draw(img, 0f, 0f)
        }

        /*shapeRenderer.inUse {
            shapeRenderer.rect(512f, 0f, 512f, 512f, Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE)
        }*/

        batch.inUse {
            batch.shader = null
            batch.color = Color.WHITE
            batch.draw(img, img.width.toFloat(), 0f)


            batch.shader = null
            font.draw(batch, "Dither level: ${dither.toInt()}", 10f, Gdx.graphics.height - 30f)
        }
    }

    override fun dispose() {
        img.dispose()
    }
}