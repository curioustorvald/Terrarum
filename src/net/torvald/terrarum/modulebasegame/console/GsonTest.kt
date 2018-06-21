package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import com.google.gson.Gson
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException

/**
 * Created by minjaesong on 2016-02-10.
 */
internal object GsonTest : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val avelem = Gson().toJsonTree((Terrarum.ingame!! as Ingame).player)

            val jsonString = avelem.toString()

            val bufferedWriter: BufferedWriter
            val writer: FileWriter
            try {
                writer = FileWriter(Terrarum.defaultDir + "/Exports/" + args[1] + ".json")
                bufferedWriter = BufferedWriter(writer)

                bufferedWriter.write(jsonString)
                bufferedWriter.close()

                Echo("GsonTest: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo("GsonTest: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {

        Echo("Usage: gsontest filename-without-extension")
    }
}
