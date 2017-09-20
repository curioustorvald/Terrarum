package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object GetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {

        val worldTime = Terrarum.ingame!!.world.time
        Echo(worldTime.getFormattedTime())
    }

    override fun printUsage() {
        Echo("Print current world time in convenient form")
    }
}