package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.ActorWithPhysics

/**
 * Created by minjaesong on 2017-01-20.
 */
internal object SetScale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2 || args.size == 3) {
            try {
                val targetID = if (args.size == 3) args[1].toInt() else (Terrarum.ingame!! as Ingame).player.referenceID
                val scale = args[if (args.size == 3) 2 else 1].toDouble()

                val target = Terrarum.ingame!!.getActorByID(targetID!!)

                if (target !is ActorWithPhysics) {
                    EchoError("Target is not ActorWithPhysics")
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