package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.StateInGame
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-15.
 */
internal object SetAV : ConsoleCommand {

    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccR = GameFontBase.colToCode["r"]
    val ccM = GameFontBase.colToCode["m"]

    override fun printUsage() {

        Echo.execute("${ccW}Set actor value of specific target to desired value.")
        Echo.execute("${ccW}Usage: ${ccY}setav ${ccG}(id) <av> <val>")
        Echo.execute("${ccW}blank ID for player. Data type will be inferred automatically.")
        Echo.execute("${ccR}Contaminated (e.g. double -> string) ActorValue will crash the game,")
        Echo.execute("${ccR}so make sure it will not happen before you issue the command!")
        Echo.execute("${ccW}Use ${ccG}__true ${ccW}and ${ccG}__false ${ccW}for boolean value.")
    }

    override fun execute(args: Array<String>) {
        fun parseAVInput(arg: String): Any {
            var `val`: Any

            try {
                `val` = Integer(arg) // try for integer
            }
            catch (e: NumberFormatException) {

                try {
                    `val` = arg.toDouble() // try for double
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


        val error = Error()

        // setav <id, or blank for player> <av> <val>
        if (args.size != 4 && args.size != 3) {
            printUsage()
        }
        else if (args.size == 3) {
            val `val` = parseAVInput(args[2])

            // check if av is number
            if (args[1].isNum()) {
                Error.execute("Illegal ActorValue ${args[1]}: ActorValue cannot be a number.")
                System.err.println("[SetAV] Illegal ActorValue ${args[1]}: ActorValue cannot be a number.")
                return
            }

            Terrarum.ingame.player.actorValue[args[1]] = `val`
            Echo.execute("${ccW}Set $ccM${args[1]} ${ccW}for ${ccY}player ${ccW}to $ccG$`val`")
            println("[SetAV] set ActorValue '${args[1]}' for player to '$`val`'.")
        }
        else if (args.size == 4) {
            try {
                val id = args[1].toInt()
                val `val` = parseAVInput(args[3])
                val actor = Terrarum.ingame.getActorByID(id)

                // check if av is number
                if (args[2].isNum()) {
                    Error.execute("Illegal ActorValue ${args[2]}: ActorValue cannot be a number.")
                    System.err.println("[SetAV] Illegal ActorValue ${args[2]}: ActorValue cannot be a number.")
                    return
                }

                actor.actorValue[args[2]] = `val`
                Echo.execute("${ccW}Set $ccM${args[2]} ${ccW}for $ccY$id ${ccW}to $ccG$`val`")
                println("[SetAV] set ActorValue '${args[2]}' for $actor to '$`val`'.")
            }
            catch (e: IllegalArgumentException) {
                if (args.size == 4) {
                    Error.execute("${args[1]}: no actor with this ID.")
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
