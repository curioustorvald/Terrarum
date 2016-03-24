package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.JsonWriter
import com.Torvald.Terrarum.Terrarum

import java.io.IOException

/**
 * Created by minjaesong on 16-02-10.
 */
class ExportAV : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                JsonWriter.writeToFile(
                        Terrarum.game.player.getActorValue(),
                        Terrarum.defaultDir + "/Exports/" + args[1] + ".json")

                Echo().execute("ExportAV: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo().execute("ExportAV: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Export ActorValue as JSON format.")
        echo.execute("Usage: exportav (id) filename-without-extension")
        echo.execute("blank ID for player")
    }
}
