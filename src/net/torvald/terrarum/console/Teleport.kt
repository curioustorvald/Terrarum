package net.torvald.terrarum.console

import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithPhysics

/**
 * Created by minjaesong on 16-01-24.
 */
internal object Teleport : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 3) {

            val x: Int
            val y: Int
            try {
                x = args[1].toInt() * FeaturesDrawer.TILE_SIZE + FeaturesDrawer.TILE_SIZE / 2
                y = args[2].toInt() * FeaturesDrawer.TILE_SIZE + FeaturesDrawer.TILE_SIZE / 2
            }
            catch (e: NumberFormatException) {
                EchoError("Teleport: wrong number input.")
                return
            }

            Terrarum.ingame!!.player!!.setPosition(x.toDouble(), y.toDouble())
        }
        else if (args.size == 4) {
            if (args[2].toLowerCase() != "to") {
                EchoError("missing 'to' on teleport command")
                return
            }
            val fromActor: ActorWithPhysics
            val targetActor: ActorWithPhysics
            try {
                val fromActorID = args[1].toInt()
                val targetActorID = if (args[3].toLowerCase() == "player")
                    Terrarum.ingame!!.player!!.referenceID
                else
                    args[3].toInt()

                // if from == target, ignore the action
                if (fromActorID == targetActorID) return

                if (Terrarum.ingame!!.getActorByID(fromActorID) !is ActorWithPhysics ||
                    Terrarum.ingame!!.getActorByID(targetActorID) !is ActorWithPhysics) {
                    throw IllegalArgumentException()
                }
                else {
                    fromActor = Terrarum.ingame!!.getActorByID(fromActorID) as ActorWithPhysics
                    targetActor = Terrarum.ingame!!.getActorByID(targetActorID) as ActorWithPhysics
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

            val actor: ActorWithPhysics
            val x: Int
            val y: Int
            try {
                x = args[3].toInt() * FeaturesDrawer.TILE_SIZE + FeaturesDrawer.TILE_SIZE / 2
                y = args[4].toInt() * FeaturesDrawer.TILE_SIZE + FeaturesDrawer.TILE_SIZE / 2
                val actorID = args[1].toInt()

                if (Terrarum.ingame!!.getActorByID(actorID) !is ActorWithPhysics) {
                    throw IllegalArgumentException()
                }
                else {
                    actor = Terrarum.ingame!!.getActorByID(actorID) as ActorWithPhysics
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
