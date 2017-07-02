package net.torvald.terrarum.console

import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import java.time.ZonedDateTime
import java.util.ArrayList
import java.util.Formatter
import java.util.regex.Pattern

/**
 * Created by minjaesong on 16-01-15.
 */
internal object CommandInterpreter {

    private val commandsNoAuth = arrayOf(
            "auth",
            "qqq",
            "zoom",
            "setlocale",
            "getlocale",
            "help",
            "version",
            "tips"
    )

    internal fun execute(command: String) {
        val cmd: Array<CommandInput?> = parse(command)

        val error = Error()

        for (single_command in cmd) {

            if (single_command == null || single_command.argsCount == 0) continue

            var commandObj: ConsoleCommand? = null
            try {
                if (single_command.name.toLowerCase().startsWith("qqq")) {
                    commandObj = CommandDict["qqq"]
                }
                else if (commandsNoAuth.contains(single_command.name.toLowerCase())) {
                    commandObj = CommandDict[single_command.name.toLowerCase()]
                }
                else {
                    if (Authenticator.b()) {
                        commandObj = CommandDict[single_command.name.toLowerCase()]
                    }
                    else {
                        // System.out.println("ee1");
                        throw NullPointerException() // if not authorised, say "Unknown command"
                    }
                }

                Echo("$ccW> $single_command") // prints out the input
                println("${ZonedDateTime.now()} [CommandInterpreter] issuing command '$single_command'")
                try {
                    commandObj.execute(single_command.toStringArray())
                }
                catch (e: Exception) {
                    System.err.print("[CommandInterpreter] ")
                    e.printStackTrace()
                    EchoError(Lang["ERROR_GENERIC_TEXT"])
                }
            }
            catch (e: NullPointerException) {
                echoUnknownCmd(single_command.name)
            }
        }
    }

    private fun parse(input: String): Array<CommandInput?> {
        val patternCommands = Pattern.compile("[^;]+")
        val patternTokensInCommand = Pattern.compile("[\"'][^;]+[\"']|[^ ]+")

        val commands = ArrayList<String>()

        // split multiple commands
        var m = patternCommands.matcher(input)
        while (m.find()) commands.add(m.group())

        // split command tokens from a command
        val parsedCommands = arrayOfNulls<CommandInput>(commands.size)


        for (i in parsedCommands.indices) {
            val tokens = ArrayList<String>()

            m = patternTokensInCommand.matcher(commands[i])
            while (m.find()) {
                val regexGroup = m.group().replace("[\"\']".toRegex(), "")
                tokens.add(regexGroup)
            }

            // create new command
            parsedCommands[i] = CommandInput(tokens.toArray())

        }

        return parsedCommands
    }

    internal fun echoUnknownCmd(cmdname: String) {
        val sb = StringBuilder()
        val formatter = Formatter(sb)

        EchoError(
                formatter.format(Lang["DEV_MESSAGE_CONSOLE_COMMAND_UNKNOWN"], cmdname).toString())
    }

    private class CommandInput(o: Array<Any>) {
        private val tokens: Array<String>

        init {
            tokens = Array<String>(o.size, { i -> o[i] as String })
        }

        fun toStringArray(): Array<String> {
            return tokens
        }

        val name: String
            get() = tokens[0]

        val argsCount: Int
            get() = tokens.size

        override fun toString(): String {
            val sb = StringBuilder()
            tokens.forEachIndexed { i, s ->
                if (i == 0)
                    sb.append("${ccY}$s${ccG}")
                else
                    sb.append(" $s")
            }
            return sb.toString()
        }
    }
}