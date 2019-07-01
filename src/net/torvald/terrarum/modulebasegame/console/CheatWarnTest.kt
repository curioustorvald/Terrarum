package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.modulebasegame.TerrarumIngame

object CheatWarnTest : ConsoleCommand {

    override fun execute(args: Array<String>) {
        (Terrarum.ingame as? TerrarumIngame)?.uiCheatMotherfuckerNootNoot?.setAsOpen()
    }

    override fun printUsage() {
    }
}