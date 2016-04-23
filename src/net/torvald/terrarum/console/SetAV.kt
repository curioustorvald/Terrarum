package net.torvald.terrarum.console

import net.torvald.terrarum.Game
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-15.
 */
internal class SetAV : ConsoleCommand {

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Set actor value of specific target to desired value.")
        echo.execute("Usage: setav (id) <av> <val>")
        echo.execute("blank ID for player")
        echo.execute("Contaminated (float -> string) actor value will crash the game,")
        echo.execute("    so make it sure before you issue the command.")
        echo.execute("Use '__true' and '__false' for boolean value.")
    }

    override fun execute(args: Array<String>) {

        fun parseAVInput(arg: String): Any {
            val `val`: Any

            try {
                `val` = Integer(arg) // try for integer
            }
            catch (e: NumberFormatException) {

                try {
                    `val` = arg.toFloat() // try for float
                }
                catch (ee: NumberFormatException) {
                    if (arg.equals("__true", ignoreCase = true)) {
                        `val` = true
                    }
                    else if (arg.equals("__false", ignoreCase = true)) {
                        `val` = false
                    }
                    else {
                        `val` = arg // string if not number
                    }
                }
            }

            return `val`
        }

        val echo = Echo()

        // setav <id, or blank for player> <av> <val>
        if (args.size != 4 && args.size != 3) {
            printUsage()
        }
        else if (args.size == 3) {
            val `val` = parseAVInput(args[2])

            // check if av is number
            if (args[1].isNum()) {
                echo.error("Illegal ActorValue “${args[1]}”: ActorValue cannot be a number.")
                return
            }

            Terrarum.game.player.actorValue[args[1]] = `val`
            echo.execute("Set ${args[1]} to $`val`")
        }
        else if (args.size == 4) {
            try {
                val id = args[1].toInt()
                val `val` = parseAVInput(args[3])

                // check if av is number
                if (args[2].isNum()) {
                    echo.error("Illegal ActorValue “${args[2]}”: ActorValue cannot be a number.")
                    return
                }

                Terrarum.game.getActor(id).actorValue[args[2]] = `val`
                echo.execute("Set ${args[2]} of $id to $`val`")
            }
            catch (e: IllegalArgumentException) {
                if (args.size == 4)
                    echo.error(args[1] + ": no actor with this ID.")
            }
        }

    }

    fun String.isNum(): Boolean {
        try {
            this.toInt()
            return true
        }
        catch (e: NumberFormatException) {
            return false
        }
    }
}
