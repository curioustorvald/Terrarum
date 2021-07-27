package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color



fun main(args: Array<String>) {
    val config = Lwjgl3ApplicationConfiguration()
    config.useVsync(false)
    config.setResizable(false)
    config.setWindowedMode(1072, 742)
    Lwjgl3Application(OpusDecodeTest, config)
}

/**
 * Created by minjaesong on 2017-07-09.
 */
object OpusDecodeTest : ApplicationAdapter() {


    override fun create() {

    }

    override fun render() {
        val color = Color(0.22f, 0.11f, 0.33f, 0f)
        println("${color.r}, ${color.g}, ${color.b}, ${color.a}")
        System.exit(0)
    }

}