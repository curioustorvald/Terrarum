package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.utils.Json
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame
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
        try {
            val world = Terrarum.ingame!!.world
            val str = WriteWorld(Terrarum.ingame!! as TerrarumIngame).invoke()
            val writer = java.io.FileWriter(AppLoader.defaultDir + "/Exports/world${world.worldIndex}.json", false)
            writer.write(str)
            writer.close()
            Echo("Exportworld: exported to world${world.worldIndex}.json")
        }
        catch (e: IOException) {
            Echo("Exportworld: IOException raised.")
            e.printStackTrace()
        }
    }

    override fun printUsage() {
        Echo("Usage: Exportworld")
    }
}