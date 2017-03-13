package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.langpack.Lang

/**
 * Created by SKYHi14 on 2017-01-31.
 */
object KillActor : ConsoleCommand {
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