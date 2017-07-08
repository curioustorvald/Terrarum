package net.torvald.terrarum.console

import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
internal object SetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val timeToSet = WorldTime.parseTime(args[1])

            Terrarum.ingame!!.world.time.setTimeOfToday(timeToSet)

            Echo("Set time to ${Terrarum.ingame!!.world.time.todaySeconds} " +
                 "(${Terrarum.ingame!!.world.time.hours}h${formatMin(Terrarum.ingame!!.world.time.minutes)})")
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