package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.serialise.ReadLayerDataLzma
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import java.io.File

/**
 * Created by minjaesong on 2017-07-18.
 */
object ImportLayerData : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            ExportLayerData.printUsage()
            return
        }

        val file = File(args[1])
        val layerData = ReadLayerDataLzma(file)


        Terrarum.ingame!!.world = GameWorldExtension(1, layerData)
        Terrarum.ingame!!.actorNowPlaying?.setPosition(
                (Terrarum.ingame!!.world).spawnY * FeaturesDrawer.TILE_SIZE.toDouble(),
                (Terrarum.ingame!!.world).spawnX * FeaturesDrawer.TILE_SIZE.toDouble()
        )

        Echo("Successfully loaded ${args[1]}")
    }

    override fun printUsage() {
        Echo("Usage: importlayer path/to/layer.data")
    }
}