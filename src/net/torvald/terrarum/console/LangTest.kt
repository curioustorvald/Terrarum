package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 16-07-11.
 */
internal object LangTest : ConsoleCommand {
    override fun printUsage() {
        Echo.execute("Prints out string in the current lang pack by STRING_ID provided")
    }

    override fun execute(args: Array<String>) {
        if (args.size < 2)
            printUsage()
        else
            Echo.execute(Lang[args[1].toUpperCase()])
    }
}