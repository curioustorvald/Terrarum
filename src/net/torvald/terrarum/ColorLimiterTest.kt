package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram

/**
 * Created by minjaesong on 2017-07-05.
 */

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    //config.useGL30 = true
    config.vSyncEnabled = false
    config.resizable = false
    config.width = 1072
    config.height = 742
    config.foregroundFPS = 9999
    LwjglApplication(ColorLimiterTest, config)
}

object ColorLimiterTest : ApplicationAdapter() {

    lateinit var img: Texture
    lateinit var shader4096: ShaderProgram

    lateinit var batch: SpriteBatch


    override fun create() {
        ShaderProgram.pedantic = false

        shader4096 = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096.frag"))
        img = Texture("assets/test_texture.tga")

        batch = SpriteBatch()
    }

    override fun render() {
        Gdx.graphics.setTitle("TestTestTest — F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glClearColor(.157f, .157f, .157f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        batch.inUse {
            batch.shader = shader4096
            //batch.shader.setUniformf("iResolution", Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

            batch.color = Color.WHITE
            batch.draw(img, 0f, 0f)


            batch.shader = null
        }
    }

    override fun dispose() {
        img.dispose()
    }
}