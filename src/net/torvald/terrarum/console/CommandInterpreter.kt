package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum
import java.time.LocalDateTime
import java.time.ZonedDateTime

import java.util.ArrayList
import java.util.Formatter
import java.util.regex.Pattern

/**
 * Created by minjaesong on 16-01-15.
 */
object CommandInterpreter {

    private val commandsNoAuth = arrayOf("auth", "qqq", "zoom", "setlocale", "getlocale", "help", "version")

    private val ccW = GameFontBase.colToCode["w"]
    private val ccG = GameFontBase.colToCode["g"]
    private val ccY = GameFontBase.colToCode["y"]
    private val ccR = GameFontBase.colToCode["r"]

    fun execute(command: String) {
        val cmd: Array<CommandInput?> = parse(command)
        val echo = Echo()

        for (single_command in cmd) {
            var commandObj: ConsoleCommand? = null
            try {
                if (commandsNoAuth.contains(single_command!!.name.toLowerCase())) {
                    commandObj = CommandDict.getCommand(single_command.name.toLowerCase())
                }
                else {
                    if (Terrarum.game.auth.b()) {
                        commandObj = CommandDict.getCommand(
                                single_command.name.toLowerCase())
                    }
                    else {
                        // System.out.println("ee1");
                        throw NullPointerException() // if not authorised, say "Unknown command"
                    }
                }
            }
            catch (e: NullPointerException) {

            }
            finally {
                echo.execute("$ccW> $single_command") // prints out the input
                println("${ZonedDateTime.now()} [CommandInterpreter] issuing command '$single_command'")
                try {
                    if (commandObj != null) {
                        commandObj.execute(single_command!!.toStringArray())
                    }
                    else {
                        echoUnknownCmd(single_command!!.name)
                        // System.out.println("ee3");
                    }
                }
                catch (e: Exception) {
                    System.err.print("[CommandInterpreter] ")
                    e.printStackTrace()
                    echo.error(Lang["ERROR_GENERIC_TEXT"])
                }

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

        Echo().error(
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