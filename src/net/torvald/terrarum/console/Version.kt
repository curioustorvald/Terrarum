package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 16-04-23.
 */
internal object Version : ConsoleCommand {
    override fun execute(args: Array<String>) {

        Echo("${TerrarumGDX.NAME} ${TerrarumGDX.VERSION_STRING}")
        Echo("Polyglot language pack version ${Lang.POLYGLOT_VERSION}")
    }

    override fun printUsage() {
        Echo("Prints out current version of the application")
    }
}