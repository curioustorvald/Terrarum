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
import com.badlogic.gdx.math.Matrix4

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
        shader4096.begin()
        //shader4096.setUniformMatrix("Bayer", Matrix4(floatArrayOf(0f,8f,2f,10f,12f,4f,14f,6f,3f,11f,1f,9f,15f,7f,13f,5f)))
        shader4096.end()


        img = Texture("assets/test_texture.tga")

        batch = SpriteBatch()
    }

    override fun render() {
        Gdx.graphics.setTitle("TestTestTest — F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glClearColor(.157f, .157f, .157f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        batch.inUse {
            batch.shader = shader4096
            //batch.shader.setUniformf("monitorGamma", 2.2f)

            batch.color = Color.WHITE
            batch.draw(img, 0f, 0f)


            batch.shader = null
        }
    }

    override fun dispose() {
        img.dispose()
    }
}