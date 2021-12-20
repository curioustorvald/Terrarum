package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2021-12-20.
 */
object Unpause : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Terrarum.ingame?.resume()
    }

    override fun printUsage() {
    }
}