package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.gameactors.ActorWBMovable

/**
 * Created by minjaesong on 2017-01-20.
 */
internal object SetScale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2 || args.size == 3) {
            try {
                val player = (Terrarum.ingame!! as Ingame).actorNowPlaying
                if (player == null) return


                val targetID = if (args.size == 3) args[1].toInt() else player.referenceID
                val scale = args[if (args.size == 3) 2 else 1].toDouble()

                val target = Terrarum.ingame!!.getActorByID(targetID!!)

                if (target !is ActorWBMovable) {
                    EchoError("Target is not ActorWBMovable")
                }
                else {
                    target.scale = scale
                }
            }
            catch (e: NumberFormatException) {
                EchoError("Wrong number input")
            }
        }
        else printUsage()
    }

    override fun printUsage() {
        Echo("Usage: setscale scale | setscale actorID scale")
    }
}