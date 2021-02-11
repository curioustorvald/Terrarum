package net.torvald.terrarum.modulebasegame.console

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.toRGBA
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
                val tileNumber = CreateTileAtlas.tileIDtoItemSheetNumber(tile)
                val colArray = CreateTileAtlas.terrainTileColourMap.get(tileNumber)!!.toByteArray()

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
    private fun Cvec.toByteArray() = this.toRGBA().toByteArray()

    private fun Int.toByteArray() = byteArrayOf(
            this.ushr(24).and(255).toByte(),
            this.ushr(16).and(255).toByte(),
            this.ushr(8).and(255).toByte(),
            this.and(255).toByte()
    )

    override fun printUsage() {

        Echo("Usage: export <name>")
        Echo("Exports current map into echo image.")
        Echo("The image can be found at %appdata%/terrarum/Exports")
    }
}


