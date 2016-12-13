package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
internal object GetTime : ConsoleCommand {
    override fun execute(args: Array<String>) {

        val worldTime = Terrarum.ingame.world.time
        Echo.execute(worldTime.getFormattedTime())
    }

    override fun printUsage() {
        Echo.execute("Print current world time in convenient form")
    }
}