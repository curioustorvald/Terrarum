package net.torvald.terrarum.console

import net.torvald.terrarum.VERSION_STRING

/**
 * Created by minjaesong on 16-04-23.
 */
class Version : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo().execute(VERSION_STRING)
    }

    override fun printUsage() {
        Echo().execute("Prints out current version of the application")
    }
}