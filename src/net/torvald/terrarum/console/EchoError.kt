package net.torvald.terrarum.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ccR

/**
 * Created by minjaesong on 2016-04-25.
 */
@ConsoleAlias("error")
internal object EchoError : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array(args.size - 1) { args[it + 1] }
        argsWoHeader.forEach { execute(it) }
    }

    fun execute(single_line: String) {
        (INGAME.consoleHandler).sendMessage("$ccR$single_line")
    }

    operator fun invoke(args: Array<String>) = execute(args)
    operator fun invoke(single_line: String) = execute(single_line)

    override fun printUsage() {

    }
}