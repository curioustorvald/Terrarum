package net.torvald.terrarum.console

/**
 * Created by minjaesong on 2016-09-07.
 */
@ConsoleAlias("println")
internal object EchoConsole : ConsoleCommand {
    /**
     * Args 0: command given
     * Args 1: first argument
     *
     * e.g. in ```setav mass 74```, zeroth args will be ```setav```.
     */
    override fun execute(args: Array<String>) {
        val argsWoHeader = Array<String>(args.size - 1, {it -> args[it + 1]})
        argsWoHeader.forEach { execute(it) }
    }

    fun execute(single_line: String) {
        val sb = StringBuilder()
        for (ch in single_line) {
            if (ch == '\n') {
                println(sb.toString())
                sb.delete(0, sb.length - 1)
            }
            else
                sb.append(ch)
        }
        println(sb.toString())
    }

    override fun printUsage() {
    }
}