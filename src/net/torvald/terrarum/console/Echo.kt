package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.ConsoleWindow

import java.util.Arrays

/**
 * Created by minjaesong on 16-01-16.
 */
internal class Echo : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array<String>(args.size - 1, {it -> args[it + 1]})

        argsWoHeader.forEach(
                { (Terrarum.game.consoleHandler.UI as ConsoleWindow).sendMessage(it) })
    }

    fun execute(single_line: String) {
        (Terrarum.game.consoleHandler.UI as ConsoleWindow).sendMessage(single_line)
    }

    override fun printUsage() {

    }
}
