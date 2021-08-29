package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2017-01-31.
 */
@ConsoleAlias("kill")
internal object KillActor : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val actorid = args[1].toInt()
                Terrarum.ingame!!.removeActor(actorid)
            }
            catch (e: NumberFormatException) {
                EchoError("Wrong number input.")
            }
            catch (e1: RuntimeException) {
                EchoError(e1.message ?: Lang["ERROR_GENERIC_TEXT"])
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: kill actorid")
    }
}