package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2021-12-20.
 */
object Pause : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Terrarum.ingame?.pause()
    }

    override fun printUsage() {
    }
}