package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.langpack.Lang

import java.util.Formatter

/**
 * Created by minjaesong on 16-01-16.
 */
class CodexEdictis : ConsoleCommand {

    val ccW = GameFontBase.colToCode["o"]

    override fun execute(args: Array<String>) {
        if (args.size == 1) {
            printList()
        }
        else {
            try {
                val commandObj = CommandDict[args[1].toLowerCase()]
                commandObj.printUsage()
            }
            catch (e: NullPointerException) {
                val sb = StringBuilder()
                val formatter = Formatter(sb)

                Echo().execute("Codex: " + formatter.format(Lang["DEV_MESSAGE_CONSOLE_COMMAND_UNKNOWN"], args[1]).toString())
            }

        }
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Usage: codex (command)")
        echo.execute("shows how to use 'command'")
        echo.execute("leave blank to get list of available commands")
    }

    private fun printList() {
        val echo = Echo()
        echo.execute(Lang["DEV_MESSAGE_CONSOLE_AVAILABLE_COMMANDS"])
        CommandDict.dict.forEach { name, cmd ->
            echo.execute("$ccW• " + name)
            cmd.printUsage()
        }
    }

}
