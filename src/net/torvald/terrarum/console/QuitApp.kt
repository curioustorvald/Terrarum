package net.torvald.terrarum.console

import com.badlogic.gdx.Gdx

/**
 * Created by minjaesong on 2016-01-15.
 */
internal object QuitApp : ConsoleCommand {

    override fun execute(args: Array<String>) {
        Gdx.app.exit()
    }

    override fun printUsage() {

    }
}
