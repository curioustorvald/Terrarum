package net.torvald.terrarum.console

import net.torvald.JsonWriter
import net.torvald.terrarum.Terrarum

import java.io.IOException

/**
 * Created by minjaesong on 16-02-10.
 */
internal object ExportAV : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                JsonWriter.writeToFile(
                        Terrarum.ingame.player.actorValue,
                        Terrarum.defaultDir + "/Exports/" + args[1] + ".json")

                Echo.execute("ExportAV: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo.execute("ExportAV: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo.execute("Export ActorValue as JSON format.")
        Echo.execute("Usage: exportav (id) filename-without-extension")
        Echo.execute("blank ID for player")
    }
}
