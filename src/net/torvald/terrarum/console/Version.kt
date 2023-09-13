package net.torvald.terrarum.console

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2016-04-23.
 */
internal object Version : ConsoleCommand {
    override fun execute(args: Array<String>) {

        Echo("${App.GAME_NAME} ${App.getVERSION_STRING()}")
        Echo("JRE Version: ${System.getProperty("java.version")}")
        Echo("Gdx Version: ${com.badlogic.gdx.Version.VERSION}")
        Echo("Polyglot language pack version: ${Lang.POLYGLOT_VERSION}")
        Echo("GL version: ${Terrarum.GL_VERSION}")
        Echo("Renderer: ${Gdx.graphics.glVersion.rendererString}, ${Gdx.graphics.glVersion.vendorString}")
    }

    override fun printUsage() {
        Echo("Prints out current version of the application")
    }
}