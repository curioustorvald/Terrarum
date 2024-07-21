package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2024-07-21.
 */
internal object SetDensity : ConsoleCommand {

    override fun printUsage() {
        Echo("${ccW}Set density of specific target to desired value.")
        Echo("${ccW}Usage: ${ccY}setdensity $ccG(id) <val>")
        Echo("${ccW}blank ID for player. Any density less than 100 will be clamped.")

    }

    override fun execute(args: Array<String>) {

        // setdensity <id, or blank for player> <av> <val>
        if (args.size != 3 && args.size != 2) {
            printUsage()
        }
        else if (args.size == 2) {
            val newValue = args[1].toDouble()

            val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
            if (player == null) {
                EchoError("Player does not exist")
                println("[SetDensity] Player does not exist")
            }
            else {
                player.density = newValue
                Echo("${ccW}Set ${ccM}density ${ccW}for ${ccY}player ${ccW}to $ccG$newValue")
                println("[SetDensity] set density '${args[1]}' for player to '$newValue'.")
            }
        }
        else if (args.size == 3) {
            try {
                val id = args[1].toInt()
                val newValue = args[2].toDouble()
                val actor = INGAME.getActorByID(id)

                if (actor is ActorWithBody) {
                    actor.density = newValue
                    Echo("${ccW}Set ${ccM}density ${ccW}for $ccY$id ${ccW}to $ccG$newValue")
                    println("[SetDensity] set density '${args[2]}' for $actor to '$newValue'.")
                }
                else {
                    EchoError("Actor ${args[1]} is not physical")
                    System.err.println("[SetDensity] Actor ${args[1]} is not physical")
                }
            }
            catch (e: IllegalArgumentException) {
                EchoError("${args[1]}: no actor with this ID.")
                System.err.println("[SetDensity] ${args[1]}: no actor with this ID.")
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