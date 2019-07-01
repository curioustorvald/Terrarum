package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object SetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val world = (Terrarum.ingame!! as TerrarumIngame).gameworld
        
        
        if (args.size == 2) {
            val timeToSet = WorldTime.parseTime(args[1])

            world.worldTime.setTimeOfToday(timeToSet)

            Echo("Set time to ${world.worldTime.todaySeconds} " +
                 "(${world.worldTime.hours}h${formatMin(world.worldTime.minutes)})")
        }
        else {
            printUsage()
        }
    }

    private fun formatMin(min: Int): String {
        return if (min < 10) "0${min.toString()}" else min.toString()
    }

    override fun printUsage() {
        Echo("usage: settime <39201-in sec or 13h32-in hour>")
    }
}