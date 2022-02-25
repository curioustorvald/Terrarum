package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2017-01-20.
 */
internal object SetScale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2 || args.size == 3) {
            try {
                val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying ?: return


                val targetID = if (args.size == 3) args[2].toInt() else player.referenceID
                val scale = args[1].toDouble()

                val target = INGAME.getActorByID(targetID)

                if (target !is ActorWithBody) {
                    EchoError("Target is not ActorWithBody")
                }
                else {
                    target.actorValue[AVKey.SCALE] = scale
                    //target.scale = scale
                }
            }
            catch (e: NumberFormatException) {
                EchoError("Wrong number input")
            }
        }
        else printUsage()
    }

    override fun printUsage() {
        Echo("Usage: setscale scale | setscale scale actorID")
    }
}