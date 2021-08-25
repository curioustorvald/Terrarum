package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object GetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {

        val worldTime = (Terrarum.ingame!!.world).worldTime
        Echo(worldTime.getFormattedTime())
    }

    override fun printUsage() {
        Echo("Print current world time in convenient form")
    }
}