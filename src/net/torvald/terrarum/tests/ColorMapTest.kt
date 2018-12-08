package net.torvald.terrarum.tests

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
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

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false//true;
    //appConfig.width = 1072; // IMAX ratio
    //appConfig.height = 742; // IMAX ratio
    appConfig.width = 1110 // photographic ratio (1.5:1)
    appConfig.height = 740 // photographic ratio (1.5:1)
    appConfig.backgroundFPS = 9999
    appConfig.foregroundFPS = 9999
    appConfig.forceExit = false

    LwjglApplication(ColorMapTest(), appConfig)
}