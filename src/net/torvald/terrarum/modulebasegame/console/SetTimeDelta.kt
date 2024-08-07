package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object SetTimeDelta : ConsoleCommand {

    override fun execute(args: Array<String>) {
        val world = (Terrarum.ingame!! as TerrarumIngame).world
        
        
        if (args.size == 2) {
            world.worldTime.timeDelta = args[1].toInt()
            if (world.worldTime.timeDelta == 0)
                Echo("時間よ止まれ！ザ・ワルド！！")
            else
                Echo("Set time delta to ${world.worldTime.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: settimedelta <int>")
    }
}