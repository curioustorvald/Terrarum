package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.serialise.ReadLayerData
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import java.io.FileInputStream

/**
 * Created by minjaesong on 2017-07-18.
 */
object ImportLayerData : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            ExportLayerData.printUsage()
            return
        }

        //val fis = GZIPInputStream(FileInputStream(args[1])) // this gzip is kaput
        val fis = FileInputStream(args[1])
        (Terrarum.ingame!!.world) = ReadLayerData(fis)
        (Terrarum.ingame!! as Ingame).playableActor.setPosition(
                (Terrarum.ingame!!.world).spawnY * FeaturesDrawer.TILE_SIZE.toDouble(),
                (Terrarum.ingame!!.world).spawnX * FeaturesDrawer.TILE_SIZE.toDouble()
        )
        fis.close()
        Echo("Successfully loaded ${args[1]}")
    }

    override fun printUsage() {
        Echo("Usage: importlayer path/to/layer.data")
    }
}