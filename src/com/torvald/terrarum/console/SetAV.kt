package com.torvald.terrarum.console

import com.torvald.terrarum.Game
import com.torvald.terrarum.Terrarum

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
        val echo = Echo()

        // setav <id or "player"> <av> <val>
        if (args.size != 4 && args.size != 3) {
            printUsage()
        }
        else if (args.size == 3) {
            val `val`: Any

            try {
                `val` = Integer(args[2]) // try for integer
            }
            catch (e: NumberFormatException) {

                try {
                    `val` = args[2].toFloat() // try for float
                }
                catch (ee: NumberFormatException) {
                    if (args[2].equals("__true", ignoreCase = true)) {
                        `val` = true
                    }
                    else if (args[2].equals("__false", ignoreCase = true)) {
                        `val` = false
                    }
                    else {
                        `val` = args[2] // string if not number
                    }
                }

            }

            Terrarum.game.player.actorValue[args[1]] = `val`
            echo.execute("Set " + args[1] + " to " + `val`)
        }
        else if (args.size == 4) {

        }

    }
}
