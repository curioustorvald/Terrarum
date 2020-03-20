package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.worlddrawer.CreateTileAtlas

/**
 * Created by minjaesong on 2016-01-24.
 */
internal object Teleport : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 3) {

            val x: Int
            val y: Int
            try {
                x = args[1].toInt() * CreateTileAtlas.TILE_SIZE + CreateTileAtlas.TILE_SIZE / 2
                y = args[2].toInt() * CreateTileAtlas.TILE_SIZE + CreateTileAtlas.TILE_SIZE / 2
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

                if (Terrarum.ingame!!.getActorByID(fromActorID) !is ActorWithBody ||
                    Terrarum.ingame!!.getActorByID(targetActorID) !is ActorWithBody) {
                    throw IllegalArgumentException()
                }
                else {
                    fromActor = Terrarum.ingame!!.getActorByID(fromActorID) as ActorWithBody
                    targetActor = Terrarum.ingame!!.getActorByID(targetActorID) as ActorWithBody
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
                x = args[3].toInt() * CreateTileAtlas.TILE_SIZE + CreateTileAtlas.TILE_SIZE / 2
                y = args[4].toInt() * CreateTileAtlas.TILE_SIZE + CreateTileAtlas.TILE_SIZE / 2
                val actorID = args[1].toInt()

                if (Terrarum.ingame!!.getActorByID(actorID) !is ActorWithBody) {
                    throw IllegalArgumentException()
                }
                else {
                    actor = Terrarum.ingame!!.getActorByID(actorID) as ActorWithBody
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
