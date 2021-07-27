package net.torvald.terrarum.tests

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.GdxColorMap

/**
 * Created by minjaesong on 2018-12-08.
 */
class ColorMapTest : Game() {

    override fun create() {

    }

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    override fun render() {
        val colormap = GdxColorMap(Gdx.files.internal("assets/testimage_resized.png"))
        println(colormap)

        System.exit(0)
    }

    override fun pause() {
        super.pause()
    }

    override fun resume() {
        super.resume()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        super.dispose()
    }
}


fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(1110, 740)
    Lwjgl3Application(ColorMapTest(), appConfig)
}