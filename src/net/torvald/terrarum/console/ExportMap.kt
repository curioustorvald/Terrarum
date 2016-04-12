package net.torvald.terrarum.console

import net.torvald.colourutil.Col4096
import net.torvald.RasterWriter
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.TileNameCode

import java.io.*
import java.util.HashMap

/**
 * Created by minjaesong on 16-01-17.
 */
class ExportMap : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0

    private val colorTable = HashMap<Int, Col4096>()

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            buildColorTable()

            var mapData = ByteArray(Terrarum.game.map.width * Terrarum.game.map.height * 3)
            var mapDataPointer = 0

            for (tile in Terrarum.game.map.terrainIterator()) {
                val colArray = (colorTable as Map<Int, Col4096>)
                        .getOrElse(tile, { Col4096(0xFFF) }).toByteArray()

                for (i in 0..2) {
                    mapData[mapDataPointer + i] = colArray[i]
                }

                mapDataPointer += 3
            }

            val dir = Terrarum.defaultDir + "/Exports/"
            val dirAsFile = File(dir)
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir()
            }

            try {
                RasterWriter.writePNG_RGB(
                        Terrarum.game.map.width, Terrarum.game.map.height, mapData, dir + args[1] + ".png")
                Echo().execute("ExportMap: exported to " + args[1] + ".png")

            }
            catch (e: IOException) {
                Echo().execute("ExportMap: IOException raised.")
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

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Usage: export <name>")
        echo.execute("Exports current map into visible image.")
        echo.execute("The image can be found at %adddata%/terrarum/Exports")
    }

    private fun buildColorTable() {
        colorTable.put(TileNameCode.AIR, Col4096(0xCEF))
        colorTable.put(TileNameCode.STONE, Col4096(0x888))
        colorTable.put(TileNameCode.DIRT, Col4096(0x753))
        colorTable.put(TileNameCode.GRASS, Col4096(0x472))

        colorTable.put(TileNameCode.ORE_COPPER, Col4096(0x6A8))
        colorTable.put(TileNameCode.ORE_IRON, Col4096(0xC75))
        colorTable.put(TileNameCode.ORE_GOLD, Col4096(0xA87))
        colorTable.put(TileNameCode.ORE_ILMENITE, Col4096(0x8AB))
        colorTable.put(TileNameCode.ORE_AURICHALCUM, Col4096(0xD92))
        colorTable.put(TileNameCode.ORE_SILVER, Col4096(0xDDD))

        colorTable.put(TileNameCode.RAW_DIAMOND, Col4096(0x2BF))
        colorTable.put(TileNameCode.RAW_RUBY, Col4096(0xB10))
        colorTable.put(TileNameCode.RAW_EMERALD, Col4096(0x0B1))
        colorTable.put(TileNameCode.RAW_SAPPHIRE, Col4096(0x01B))
        colorTable.put(TileNameCode.RAW_TOPAZ, Col4096(0xC70))
        colorTable.put(TileNameCode.RAW_AMETHYST, Col4096(0x70C))

        colorTable.put(TileNameCode.WATER, Col4096(0x038))
        colorTable.put(TileNameCode.LAVA, Col4096(0xF50))

        colorTable.put(TileNameCode.SAND, Col4096(0xDDB))
        colorTable.put(TileNameCode.SAND_WHITE, Col4096(0xFFD))
        colorTable.put(TileNameCode.SAND_RED, Col4096(0xA32))
        colorTable.put(TileNameCode.SAND_DESERT, Col4096(0xEDB))
        colorTable.put(TileNameCode.SAND_BLACK, Col4096(0x444))
        colorTable.put(TileNameCode.SAND_GREEN, Col4096(0x9A6))

        colorTable.put(TileNameCode.GRAVEL, Col4096(0x664))
        colorTable.put(TileNameCode.GRAVEL_GREY, Col4096(0x999))

        colorTable.put(TileNameCode.ICE_NATURAL, Col4096(0x9AB))
        colorTable.put(TileNameCode.ICE_MAGICAL, Col4096(0x7AC))
        colorTable.put(TileNameCode.ICE_FRAGILE, Col4096(0x6AF))
        colorTable.put(TileNameCode.SNOW, Col4096(0xCDE))


    }
}


