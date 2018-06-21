package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2016-07-11.
 */
internal object LangTest : ConsoleCommand {
    override fun printUsage() {
        Echo("Prints out string in the current lang pack by STRING_ID provided")
    }

    override fun execute(args: Array<String>) {
        if (args.size < 2)
            printUsage()
        else
            Echo(Lang[args[1].toUpperCase()])
    }
}