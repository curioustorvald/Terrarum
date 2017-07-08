package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.ConsoleWindow

/**
 * Created by minjaesong on 16-04-25.
 */
internal object EchoError : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array<String>(args.size - 1, {it -> args[it + 1]})
        argsWoHeader.forEach { execute(it) }
    }

    fun execute(single_line: String) {
        (Terrarum.ingame!!.consoleHandler.UI as ConsoleWindow).sendMessage(single_line)
    }

    operator fun invoke(args: Array<String>) = execute(args)
    operator fun invoke(single_line: String) = execute(single_line)

    override fun printUsage() {

    }
}