package net.torvald.terrarum.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music

/**
 * Created by minjaesong on 2016-08-02.
 */
internal object MusicTest : ConsoleCommand {

    var music: Music? = null

    /**
     * Args 0: command given
     * Args 1: first argument
     *
     * e.g. in ```setav mass 74```, zeroth args will be ```setav```.
     */
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            printUsage()
            return
        }

        if (args[1] == "stop") {
            music!!.stop()
            return
        }

        val type = args[1].substringAfter('.').toUpperCase()
        /*AudioLoader.getStreamingAudio(
                type,
                File("./assets/sounds/test/${args[1]}").absoluteFile.toURI().toURL()
        ).playAsMusic(1f, 1f, false)*/

        music = Gdx.audio.newMusic(Gdx.files.internal("./assets/sounds/test/${args[1]}"))
        music!!.play()
    }

    override fun printUsage() {
        Echo("Usage: musictest filename/in/res/sounds/test")
        Echo("musictest stop to stop playback")
    }
}