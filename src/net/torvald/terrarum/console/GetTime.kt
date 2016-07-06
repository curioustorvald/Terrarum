package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class GetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val echo = Echo()
        val worldTime = Terrarum.ingame.map.worldTime
        echo.execute("Year ${worldTime.years}, Month ${worldTime.months}, " +
                     "Day ${worldTime.days} (${worldTime.getDayNameShort()}), " +
                     "${worldTime.getFormattedTime()}"
        )
    }

    override fun printUsage() {
        Echo().execute("Print current world time in convenient form")
    }
}