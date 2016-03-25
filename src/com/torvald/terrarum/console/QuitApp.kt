package com.torvald.terrarum.console

/**
 * Created by minjaesong on 16-01-15.
 */
class QuitApp : ConsoleCommand {

    override fun execute(args: Array<String>) {
        System.exit(1)
    }

    override fun printUsage() {

    }
}
