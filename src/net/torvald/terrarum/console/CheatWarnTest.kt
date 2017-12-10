package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

object CheatWarnTest : ConsoleCommand {

    override fun execute(args: Array<String>) {
        Terrarum.ingame?.uiCheatMotherfuckerNootNoot?.setAsOpen()
    }

    override fun printUsage() {
    }
}