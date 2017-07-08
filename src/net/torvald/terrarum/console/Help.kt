package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 16-03-22.
 */
internal object Help : ConsoleCommand {
    override fun execute(args: Array<String>) {

        if (args.size == 1) {
            for (i in 1..6) Echo(Lang["HELP_OTF_MAIN_$i"])
        }
        else if (args[1].toLowerCase() == "slow") {
            for (i in 1..4) Echo(Lang["HELP_OTF_SLOW_$i"])
        }
        else {
            for (i in 1..6) Echo(Lang["HELP_OTF_MAIN_$i"])
        }
    }

    override fun printUsage() {
        Echo("Prints some utility functions assigned to function row of the keyboard.")
    }
}