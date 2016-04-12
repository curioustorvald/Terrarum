package net.torvald.terrarum.console

import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class SetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val lowercaseTime = args[1].toLowerCase()
            val timeToSet =
                    if (args[1].length >= 4) {
                        lowercaseTime.substringBefore('h').toInt() * WorldTime.HOUR_SEC +
                        lowercaseTime.substringAfter('h').toInt() * WorldTime.MINUTE_SEC
                    }
                    else if (args[1].endsWith("h", true)) {
                        lowercaseTime.substring(0, args[1].length - 1).toInt() * WorldTime.HOUR_SEC
                    }
                    else {
                        lowercaseTime.toInt()
                    }

            Terrarum.game.map.worldTime.setTime(timeToSet)

            Echo().execute("Set time to ${Terrarum.game.map.worldTime.elapsedSeconds()} " +
                           "(${Terrarum.game.map.worldTime.hours}h${formatMin(Terrarum.game.map.worldTime.minutes)})")
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