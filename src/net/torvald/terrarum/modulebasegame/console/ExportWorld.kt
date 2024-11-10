package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.TitlescreenGameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.serialise.WriteTitlescreenGameWorld
import java.io.File
import java.io.IOException

/**
 * Used to create the titlescreen world
 *
 * Created by minjaesong on 2023-10-04.
 */
object ExportWorld : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val ingame = Terrarum.ingame!! as TerrarumIngame
                val file = File(App.defaultDir + "/Exports/${args[1]}.json")
                val simpleworld = TitlescreenGameWorld(ingame.world.width, ingame.world.height).also {
                    it.layerTerrain = ingame.world.layerTerrain
                    it.layerWall = ingame.world.layerWall
                    it.tileNumberToNameMap.putAll(ingame.world.tileNumberToNameMap)
                }
                file.writeText(WriteTitlescreenGameWorld(ingame, simpleworld, listOf()))
                Echo("Exportworld: exported the world as ${args[1]}.json")
            }
            catch (e: IOException) {
                Echo("Exportworld: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: exportworld filename-without-extension")
    }
}