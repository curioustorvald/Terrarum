package net.torvald.terrarum.console

import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class SetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val timeToSet = WorldTime.parseTime(args[1])

            Terrarum.ingame.map.worldTime.setTime(timeToSet)

            Echo().execute("Set time to ${Terrarum.ingame.map.worldTime.elapsedSeconds()} " +
                           "(${Terrarum.ingame.map.worldTime.hours}h${formatMin(Terrarum.ingame.map.worldTime.minutes)})")
        }
        else {
            printUsage()
        }
    }

    private fun formatMin(min: Int): String {
        return if (min < 10) "0${min.toString()}" else min.toString()
    }

    override fun printUsage() {
        Echo().execute("usage: settime <39201-in sec or 13h32-in hour>")
    }
}