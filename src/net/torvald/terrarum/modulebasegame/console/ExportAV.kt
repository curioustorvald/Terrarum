package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.utils.JsonWriter
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame

import java.io.IOException

/**
 * Created by minjaesong on 2016-02-10.
 */
internal object ExportAV : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                if (player == null) return

                JsonWriter.writeToFile(
                        player,
                        App.defaultDir + "/Exports/" + args[1] + ".json")

                Echo("ExportAV: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo("ExportAV: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Export ActorValue as JSON format.")
        Echo("Usage: exportav (id) filename-without-extension")
        Echo("blank ID for player")
    }
}
