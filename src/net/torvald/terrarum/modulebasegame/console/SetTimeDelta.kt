package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object SetTimeDelta : ConsoleCommand {

    val HARD_LIMIT = 60

    override fun execute(args: Array<String>) {
        val world = (Terrarum.ingame!! as Ingame).world
        
        
        if (args.size == 2) {
            world.time.timeDelta = args[1].toInt()
            if (world.time.timeDelta == 0)
                Echo("時間よ止まれ！ザ・ワルド！！")
            else
                Echo("Set time delta to ${world.time.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: settimedelta <int>")
    }
}