package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.serialise.ReadLayerData
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

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
        Terrarum.ingame!!.world = ReadLayerData(fis)
        Terrarum.ingame!!.player.setPosition(
                Terrarum.ingame!!.world.spawnY * FeaturesDrawer.TILE_SIZE.toDouble(),
                Terrarum.ingame!!.world.spawnX * FeaturesDrawer.TILE_SIZE.toDouble()
        )
        fis.close()
        Echo("Successfully loaded ${args[1]}")
    }

    override fun printUsage() {
        Echo("Usage: importlayer path/to/layer.data")
    }
}