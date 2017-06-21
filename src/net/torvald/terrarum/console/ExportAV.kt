package net.torvald.terrarum.console

import net.torvald.terrarum.utils.JsonWriter
import net.torvald.terrarum.TerrarumGDX

import java.io.IOException

/**
 * Created by minjaesong on 16-02-10.
 */
internal object ExportAV : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                JsonWriter.writeToFile(
                        TerrarumGDX.ingame!!.player!!.actorValue,
                        TerrarumGDX.defaultDir + "/Exports/" + args[1] + ".json")

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
