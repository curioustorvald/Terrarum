package net.torvald.terrarum.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccM
import net.torvald.terrarum.ccW
import net.torvald.terrarum.modulebasegame.console.SetAV.isNum

/**
 * Created by minjaesong on 2022-06-03.
 */
internal object SetGR : ConsoleCommand {
    override fun execute(args: Array<String>) {
        fun parseAVInput(arg: String): Any {
            var inputval: Any

            try {
                inputval = Integer(arg) // try for integer
            }
            catch (e: NumberFormatException) {

                try {
                    inputval = arg.toDouble() // try for double
                }
                catch (ee: NumberFormatException) {
                    if (arg.equals("__true", ignoreCase = true)) {
                        inputval = true
                    }
                    else if (arg.equals("__false", ignoreCase = true)) {
                        inputval = false
                    }
                    else {
                        inputval = arg // string if not number
                    }
                }
            }

            return inputval
        }

        // setav <id, or blank for player> <av> <val>
        if (args.size != 3) {
            printUsage()
        }
        else if (args.size == 3) {
            val newValue = parseAVInput(args[2])

            // check if av is number
            if (args[1].isNum()) {
                EchoError("Illegal Gamerule ${args[1]}: Gamerule cannot be a number.")
                System.err.println("[SetGR] Illegal Gamerule ${args[1]}: Gamerule cannot be a number.")
                return
            }

            INGAME.world.gameRules[args[1]] = newValue
            Echo("${ccW}Set Gamerule $ccM${args[1]} ${ccW}to $ccG$newValue")
            println("[SetGR] set Gamerule '${args[1]}' to '$newValue'.")
        }
    }

    override fun printUsage() {
        Echo("Usage: setgr <gamerule> <value>")
    }
}