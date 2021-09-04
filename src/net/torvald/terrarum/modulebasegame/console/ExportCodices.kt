package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.serialise.Common

/**
 * Created by minjaesong on 2021-09-04.
 */
object ExportCodices : ConsoleCommand {
    override fun execute(args: Array<String>) {
        println(Common.jsoner.toJson(Terrarum.BlockCodex))
    }

    override fun printUsage() {
        TODO("Not yet implemented")
    }
}