package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.Terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class GetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val echo = Echo()
        echo.execute("Day ${Terrarum.game.map.worldTime.days}, " +
                     "${Terrarum.game.map.worldTime.getFormattedTime()} " +
                     "(${Terrarum.game.map.worldTime.elapsedSeconds()} s)"
        )
    }

    override fun printUsage() {
        Echo().execute("Print current world time in convenient form")
    }
}