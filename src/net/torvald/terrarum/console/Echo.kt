package net.torvald.terrarum.console

import net.torvald.terrarum.*
import net.torvald.terrarum.App.csi0
import net.torvald.terrarum.App.csiG
import net.torvald.terrarum.ui.ConsoleWindow

/**
 * Created by minjaesong on 2016-01-16.
 */
internal object Echo : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array<String>(args.size - 1, {it -> args[it + 1]})
        argsWoHeader.forEach { execute(it) }
    }

    fun execute(single_line: String) {
        val sb = StringBuilder()
        for (ch in single_line) {
            if (ch == '\n') {
                INGAME.consoleHandler.sendMessage(sb.toString())
                println("[Echo] $csiG$sb$csi0")
                sb.delete(0, sb.length - 1)
            }
            else
                sb.append(ch)
        }
        INGAME.consoleHandler.sendMessage(sb.toString())
        println("[Echo] $csiG$sb$csi0")
    }

    operator fun invoke(args: Array<String>) = execute(args)
    operator fun invoke(single_line: String) = execute(single_line)

    override fun printUsage() {

    }
}
