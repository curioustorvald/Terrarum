package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.Terrarum.Game
import com.Torvald.Terrarum.LangPack.Lang
import com.Torvald.Terrarum.UserInterface.ConsoleWindow

import java.util.Formatter

/**
 * Created by minjaesong on 16-01-16.
 */
class CodexEdictis : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 1) {
            printList()
        }
        else {
            try {
                val commandObj = CommandDict.getCommand(args[1].toLowerCase())
                commandObj.printUsage()
            }
            catch (e: NullPointerException) {
                val sb = StringBuilder()
                val formatter = Formatter(sb)

                Echo().execute("Codex: " + formatter.format(Lang.get("DEV_MESSAGE_CONSOLE_COMMAND_UNKNOWN"), args[1]).toString())
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
        echo.execute(Lang.get("DEV_MESSAGE_CONSOLE_AVAILABLE_COMMANDS"))
        CommandDict.dict.keys.forEach { s -> echo.execute("] " + s) }
    }

}
