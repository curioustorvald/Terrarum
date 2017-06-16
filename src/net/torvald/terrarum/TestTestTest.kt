package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarumsansbitmap.gdx.GameFontBase


/**
 * Created by minjaesong on 2017-06-11.
 */
class TestTestTest : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var img: Texture

    lateinit var gameFont: BitmapFont

    override fun create() {
        batch = SpriteBatch()
        img = Texture("assets/test_texture.tga")

        gameFont = GameFontBase(false)
        //gameFont = BitmapFont()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        Gdx.graphics.setTitle("$GAME_NAME — F: ${Gdx.graphics.framesPerSecond}")

        batch.inBatch {

            gameFont.draw(batch, "Hello, world!", 10f, 30f)
        }
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }


    private inline fun SpriteBatch.inBatch(action: () -> Unit) {
        this.begin()
        action()
        this.end()
    }
}

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    config.useGL30 = true
    config.vSyncEnabled = false
    //config.foregroundFPS = 9999
    LwjglApplication(TestTestTest(), config)
}