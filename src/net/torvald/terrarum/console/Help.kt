package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 16-03-22.
 */
class Help : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val echo = Echo()
        if (args.size == 1) {
            for (i in 1..6) echo.execute(Lang["HELP_OTF_MAIN_$i"])
        }
        else if (args[1].toLowerCase() == "slow") {
            for (i in 1..4) echo.execute(Lang["HELP_OTF_SLOW_$i"])
        }
        else {
            for (i in 1..6) echo.execute(Lang["HELP_OTF_MAIN_$i"])
        }
    }

    override fun printUsage() {
        Echo().execute("Prints some utility functions assigned to function row of the keyboard.")
    }
}