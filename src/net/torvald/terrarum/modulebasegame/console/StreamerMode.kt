package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2021-10-06.
 */
object StreamerMode : ConsoleCommand {
    override fun execute(args: Array<String>) {
        App.setConfig("fx_streamerslayout", !App.getConfigBoolean("fx_streamerslayout"))
    }

    override fun printUsage() {
        Echo("Toggles Streamer Mode")
    }
}