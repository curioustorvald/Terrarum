package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-04-23.
 */
class Version : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo().execute("${Terrarum.NAME} ${Terrarum.VERSION_STRING}")
    }

    override fun printUsage() {
        Echo().execute("Prints out current version of the application")
    }
}