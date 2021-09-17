package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-01-24.
 */
@ConsoleAlias("tp,goto")
internal object Teleport : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 3) {

            val x: Int
            val y: Int
            try {
                x = args[1].toInt() * TILE_SIZE + TILE_SIZE / 2
                y = args[2].toInt() * TILE_SIZE + TILE_SIZE / 2
            }
            catch (e: NumberFormatException) {
                EchoError("Teleport: wrong number input.")
                return
            }

            (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.setPosition(x.toDouble(), y.toDouble())
        }
        else if (args.size == 4) {
            if (args[2].toLowerCase() != "to") {
                EchoError("missing 'to' on teleport command")
                return
            }
            val fromActor: ActorWithBody
            val targetActor: ActorWithBody
            try {
                val fromActorID = args[1].toInt()
                val targetActorID = if (args[3].toLowerCase() == "player") {
                    val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                    if (player == null) {
                        EchoError("Player does not exist")
                        return
                    }
                    else
                        player.referenceID!!
                }
                else
                    args[3].toInt()

                // if from == target, ignore the action
                if (fromActorID == targetActorID) return

                if (INGAME.getActorByID(fromActorID) !is ActorWithBody ||
                    INGAME.getActorByID(targetActorID) !is ActorWithBody) {
                    throw IllegalArgumentException()
                }
                else {
                    fromActor = INGAME.getActorByID(fromActorID) as ActorWithBody
                    targetActor = INGAME.getActorByID(targetActorID) as ActorWithBody
                }
            }
            catch (e: NumberFormatException) {
                EchoError("Teleport: illegal number input")
                return
            }
            catch (e1: IllegalArgumentException) {
                EchoError("Teleport: operation not possible on specified actor(s)")
                return
            }

            fromActor.setPosition(
                    targetActor.feetPosVector.x,
                    targetActor.feetPosVector.y
            )
        }
        else if (args.size == 5) {
            if (args[2].toLowerCase() != "to") {
                EchoError("missing 'to' on teleport command")
                return
            }

            val actor: ActorWithBody
            val x: Int
            val y: Int
            try {
                x = args[3].toInt() * TILE_SIZE + TILE_SIZE / 2
                y = args[4].toInt() * TILE_SIZE + TILE_SIZE / 2
                val actorID = args[1].toInt()

                if (INGAME.getActorByID(actorID) !is ActorWithBody) {
                    throw IllegalArgumentException()
                }
                else {
                    actor = INGAME.getActorByID(actorID) as ActorWithBody
                }
            }
            catch (e: NumberFormatException) {
                EchoError("Teleport: illegal number input")
                return
            }
            catch (e1: IllegalArgumentException) {
                EchoError("Teleport: operation not possible on specified actor")
                return
            }

            actor.setPosition(x.toDouble(), y.toDouble())
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: teleport x-tile y-tile")
        Echo("    teleport actorid to x-tile y-tile")
        Echo("    teleport actorid to actorid")
        Echo("    teleport actorid to player")
    }
}
