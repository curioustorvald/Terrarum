package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.ConsoleWindow

import java.util.Arrays

/**
 * Created by minjaesong on 16-01-16.
 */
internal class Echo : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array<String>(args.size - 1, {it -> args[it + 1]})
        argsWoHeader.forEach { execute(it) }
    }

    fun execute(single_line: String) {
        val sb = StringBuilder()
        for (ch in single_line) {
            if (ch == '\n') {
                (Terrarum.ingame.consoleHandler.UI as ConsoleWindow).sendMessage(sb.toString())
                sb.delete(0, sb.length - 1)
            }
            else
                sb.append(ch)
        }
        (Terrarum.ingame.consoleHandler.UI as ConsoleWindow).sendMessage(sb.toString())
    }

    override fun printUsage() {

    }
}
