package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.Terrarum.Terrarum
import com.google.gson.Gson
import com.google.gson.JsonElement

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException

/**
 * Created by minjaesong on 16-02-10.
 */
class GsonTest : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val avelem = Gson().toJsonTree(Terrarum.game.player)

            val jsonString = avelem.toString()

            val bufferedWriter: BufferedWriter
            val writer: FileWriter
            try {
                writer = FileWriter(Terrarum.defaultDir + "/Exports/" + args[1] + ".json")
                bufferedWriter = BufferedWriter(writer)

                bufferedWriter.write(jsonString)
                bufferedWriter.close()

                Echo().execute("GsonTest: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo().execute("GsonTest: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Usage: gsontest filename-without-extension")
    }
}
