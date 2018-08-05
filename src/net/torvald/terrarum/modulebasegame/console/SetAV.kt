package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-01-15.
 */
internal object SetAV : ConsoleCommand {

    override fun printUsage() {
        Echo("${ccW}Set actor value of specific target to desired value.")
        Echo("${ccW}Usage: ${ccY}setav ${ccG}(id) <av> <val>")
        Echo("${ccW}blank ID for player. Data type will be inferred automatically.")
        Echo("${ccR}Contaminated (e.g. double -> string) ActorValue will crash the game,")
        Echo("${ccR}so make sure it will not happen before you issue the command!")
        Echo("${ccW}Use ${ccG}__true ${ccW}and ${ccG}__false ${ccW}for boolean value.")
    }

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
        if (args.size != 4 && args.size != 3) {
            printUsage()
        }
        else if (args.size == 3) {
            val newValue = parseAVInput(args[2])

            // check if av is number
            if (args[1].isNum()) {
                EchoError("Illegal ActorValue ${args[1]}: ActorValue cannot be a number.")
                System.err.println("[SetAV] Illegal ActorValue ${args[1]}: ActorValue cannot be a number.")
                return
            }

            (Terrarum.ingame!! as Ingame).playableActor.actorValue[args[1]] = newValue
            Echo("${ccW}Set $ccM${args[1]} ${ccW}for ${ccY}player ${ccW}to $ccG$newValue")
            println("[SetAV] set ActorValue '${args[1]}' for player to '$newValue'.")
        }
        else if (args.size == 4) {
            try {
                val id = args[1].toInt()
                val newValue = parseAVInput(args[3])
                val actor = Terrarum.ingame!!.getActorByID(id)

                // check if av is number
                if (args[2].isNum()) {
                    EchoError("Illegal ActorValue ${args[2]}: ActorValue cannot be a number.")
                    System.err.println("[SetAV] Illegal ActorValue ${args[2]}: ActorValue cannot be a number.")
                    return
                }

                actor.actorValue[args[2]] = newValue
                Echo("${ccW}Set $ccM${args[2]} ${ccW}for $ccY$id ${ccW}to $ccG$newValue")
                println("[SetAV] set ActorValue '${args[2]}' for $actor to '$newValue'.")
            }
            catch (e: IllegalArgumentException) {
                if (args.size == 4) {
                    EchoError("${args[1]}: no actor with this ID.")
                    System.err.println("[SetAV] ${args[1]}: no actor with this ID.")
                }
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
