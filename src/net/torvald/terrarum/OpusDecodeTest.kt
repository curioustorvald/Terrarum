package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.glester.jopus.JOpusBufferFile



fun main(args: Array<String>) {


    val config = LwjglApplicationConfiguration()
    LwjglApplication(OpusDecodeTest, config)
}

/**
 * Created by minjaesong on 2017-07-09.
 */
object OpusDecodeTest : ApplicationAdapter() {


    private lateinit var opusFile: JOpusBufferFile

    override fun create() {

    }

    override fun render() {
        val color = Color(0.22f, 0.11f, 0.33f, 0f)
        println("${color.r}, ${color.g}, ${color.b}, ${color.a}")
        System.exit(0)
    }

}