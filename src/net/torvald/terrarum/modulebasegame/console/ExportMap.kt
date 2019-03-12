package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2016-01-17.
 */
internal object ExportMap : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0



    override fun execute(args: Array<String>) {
        val world = (Terrarum.ingame!!.world)
        
        if (args.size == 2) {

            // TODO rewrite to use Pixmap and PixmapIO

            var mapData = ByteArray(world.width * world.height * 3)
            var mapDataPointer = 0

            for (tile in world.terrainIterator()) {
                val colArray = CreateTileAtlas.terrainTileColourMap.getRaw(tile % 16, tile / 16).toByteArray()

                for (i in 0..2) {
                    mapData[mapDataPointer + i] = colArray[i]
                }

                mapDataPointer += 3
            }

            val dir = AppLoader.defaultDir + "/Exports/"
            val dirAsFile = File(dir)
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir()
            }

            try {
                RasterWriter.writePNG_RGB(
                        world.width, world.height, mapData, dir + args[1] + ".png")
                Echo("ExportMap: exported to " + args[1] + ".png")

            }
            catch (e: IOException) {
                EchoError("ExportMap: IOException raised.")
                e.printStackTrace()
            }

            // mapData = null
            // mapDataPointer = 0

            // Free up some memory
            System.gc()
        }
        else {
            printUsage()
        }
    }

    /***
     * R-G-B-A order for RGBA input value
     */
    private fun Int.toByteArray() = byteArrayOf(
            this.shl(24).and(0xff).toByte(),
            this.shl(16).and(0xff).toByte(),
            this.shl(8).and(0xff).toByte(),
            this.and(0xff).toByte()
    )

    override fun printUsage() {

        Echo("Usage: export <name>")
        Echo("Exports current map into echo image.")
        Echo("The image can be found at %appdata%/terrarum/Exports")
    }
}


