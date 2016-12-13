package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 16-04-23.
 */
internal object Version : ConsoleCommand {
    override fun execute(args: Array<String>) {

        Echo.execute("${Terrarum.NAME} ${Terrarum.VERSION_STRING}")
        Echo.execute("Polyglot language pack version ${Lang.POLYGLOT_VERSION}")
    }

    override fun printUsage() {
        Echo.execute("Prints out current version of the application")
    }
}