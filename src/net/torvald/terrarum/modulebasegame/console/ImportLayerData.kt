package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import java.io.File

/**
 * Created by minjaesong on 2017-07-18.
 */
object ImportLayerData : ConsoleCommand {
    override fun execute(args: Array<String>) {
        /*if (args.size < 2) {
            ExportLayerData.printUsage()
            return
        }

        val file = File(args[1])
        val layerData = ReadLayerDataZip(file)


        Terrarum.ingame!!.world = GameWorldExtension(1, layerData, 0, 0, 0) // FIXME null TIME_T for the (partial) test to pass
        Terrarum.ingame!!.actorNowPlaying?.setPosition(
                (Terrarum.ingame!!.world).spawnY * TILE_SIZED,
                (Terrarum.ingame!!.world).spawnX * TILE_SIZED
        )

        Echo("Successfully loaded ${args[1]}")*/
    }

    override fun printUsage() {
        Echo("Usage: importlayer path/to/layer.data")
    }
}