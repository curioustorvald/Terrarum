package net.torvald.terrarum.console

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2016-04-23.
 */
internal object Version : ConsoleCommand {
    override fun execute(args: Array<String>) {

        Echo("${Terrarum.NAME} ${AppLoader.getVERSION_STRING()}")
        Echo("Polyglot language pack version ${Lang.POLYGLOT_VERSION}")
        Echo("GL_VERSION: ${Terrarum.GL_VERSION}")
        Echo("Renderer: ${Gdx.graphics.glVersion.rendererString}, ${Gdx.graphics.glVersion.vendorString}")
    }

    override fun printUsage() {
        Echo("Prints out current version of the application")
    }
}