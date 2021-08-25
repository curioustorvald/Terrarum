package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.utils.Json
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.serialise.WriteActor
import net.torvald.terrarum.serialise.WriteMeta
import net.torvald.terrarum.serialise.WriteWorld
import net.torvald.terrarum.utils.JsonWriter
import java.io.IOException

/**
 * Created by minjaesong on 2017-07-18.
 */
object ExportMeta : ConsoleCommand {
    override fun execute(args: Array<String>) {
        try {
            val str = WriteMeta(Terrarum.ingame!! as TerrarumIngame).invoke()
            val writer = java.io.FileWriter(AppLoader.defaultDir + "/Exports/savegame.json", false)
            writer.write(str)
            writer.close()
            Echo("Exportmeta: exported to savegame.json")
        }
        catch (e: IOException) {
            Echo("Exportmeta: IOException raised.")
            e.printStackTrace()
        }
    }

    override fun printUsage() {
        Echo("Usage: Exportmeta")
    }
}

object ExportWorld : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val str = WriteWorld(Terrarum.ingame!! as TerrarumIngame).invoke()
                val writer = java.io.FileWriter(AppLoader.defaultDir + "/Exports/${args[1]}", false)
                writer.write(str)
                writer.close()
                Echo("Exportworld: exported to ${args[1]}")
            }
            catch (e: IOException) {
                Echo("Exportworld: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            ImportWorld.printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: Exportworld filename.json")
    }
}

object ExportActor : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                if (player == null) return

                val str = WriteActor(player)
                val writer = java.io.FileWriter(AppLoader.defaultDir + "/Exports/${args[1]}.json", false)
                writer.write(str)
                writer.close()

                Echo("Exportactor: exported to ${args[1]}.json")
            }
            catch (e: IOException) {
                Echo("Exportactor: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Export Actor as JSON format.")
        Echo("Usage: exportactor (id) filename-without-extension")
        Echo("blank ID for player")
    }
}