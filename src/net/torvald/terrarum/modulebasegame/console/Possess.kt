package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2021-08-29.
 */
object Possess : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val ingame = Terrarum.ingame!! as TerrarumIngame
            if (args[1].lowercase() == "reset")
                ingame.changePossession(ingame.actorGamer)
            else
                ingame.changePossession(args[1].toInt())
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: possess (id) | possess reset")
    }

}