package net.torvald.terrarum.console

import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-03-20.
 */
internal object SetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val timeToSet = WorldTime.parseTime(args[1])

            TerrarumGDX.ingame!!.world.time.setTimeOfToday(timeToSet)

            Echo("Set time to ${TerrarumGDX.ingame!!.world.time.todaySeconds} " +
                 "(${TerrarumGDX.ingame!!.world.time.hours}h${formatMin(TerrarumGDX.ingame!!.world.time.minutes)})")
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