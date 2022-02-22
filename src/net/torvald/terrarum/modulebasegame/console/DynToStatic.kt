package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.serialise.Common

/**
 * Created by minjaesong on 2022-02-22.
 */
object DynToStatic : ConsoleCommand {
    override fun execute(args: Array<String>) {
        ItemCodex.dynamicToStaticTable.forEach { (d,s) ->
            Echo("$ccG$d$ccW → $ccY$s")
        }
    }

    override fun printUsage() {
    }
}