package net.torvald.terrarum.console

import net.torvald.colourutil.Col4096
import net.torvald.RasterWriter
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.Tile

import java.io.*
import java.util.HashMap

/**
 * Created by minjaesong on 16-01-17.
 */
internal object ExportMap : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0

    private val colorTable = HashMap<Int, Col4096>()

    init {
        colorTable.put(Tile.AIR, Col4096(0xCEF))
        colorTable.put(Tile.STONE, Col4096(0x888))
        colorTable.put(Tile.DIRT, Col4096(0x753))
        colorTable.put(Tile.GRASS, Col4096(0x472))

        colorTable.put(Tile.ORE_COPPER, Col4096(0x6A8))
        colorTable.put(Tile.ORE_IRON, Col4096(0xC75))
        colorTable.put(Tile.ORE_GOLD, Col4096(0xA87))
        colorTable.put(Tile.ORE_ILMENITE, Col4096(0x8AB))
        colorTable.put(Tile.ORE_AURICHALCUM, Col4096(0xD92))
        colorTable.put(Tile.ORE_SILVER, Col4096(0xDDD))

        colorTable.put(Tile.RAW_DIAMOND, Col4096(0x2BF))
        colorTable.put(Tile.RAW_RUBY, Col4096(0xB10))
        colorTable.put(Tile.RAW_EMERALD, Col4096(0x0B1))
        colorTable.put(Tile.RAW_SAPPHIRE, Col4096(0x01B))
        colorTable.put(Tile.RAW_TOPAZ, Col4096(0xC70))
        colorTable.put(Tile.RAW_AMETHYST, Col4096(0x70C))

        colorTable.put(Tile.WATER, Col4096(0x038))
        colorTable.put(Tile.LAVA, Col4096(0xF50))

        colorTable.put(Tile.SAND, Col4096(0xDDB))
        colorTable.put(Tile.SAND_WHITE, Col4096(0xFFD))
        colorTable.put(Tile.SAND_RED, Col4096(0xA32))
        colorTable.put(Tile.SAND_DESERT, Col4096(0xEDB))
        colorTable.put(Tile.SAND_BLACK, Col4096(0x444))
        colorTable.put(Tile.SAND_GREEN, Col4096(0x9A6))

        colorTable.put(Tile.GRAVEL, Col4096(0x664))
        colorTable.put(Tile.GRAVEL_GREY, Col4096(0x999))

        colorTable.put(Tile.ICE_NATURAL, Col4096(0x9AB))
        colorTable.put(Tile.ICE_MAGICAL, Col4096(0x7AC))
        colorTable.put(Tile.ICE_FRAGILE, Col4096(0x6AF))
        colorTable.put(Tile.SNOW, Col4096(0xCDE))
    }

    override fun execute(args: Array<String>) {
        if (args.size == 2) {

            var mapData = ByteArray(Terrarum.ingame!!.world.width * Terrarum.ingame!!.world.height * 3)
            var mapDataPointer = 0

            for (tile in Terrarum.ingame!!.world.terrainIterator()) {
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
                        Terrarum.ingame!!.world.width, Terrarum.ingame!!.world.height, mapData, dir + args[1] + ".png")
                Echo("ExportMap: exported to " + args[1] + ".png")

            }
            catch (e: IOException) {
                Echo("ExportMap: IOException raised.")
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

        Echo("Usage: export <name>")
        Echo("Exports current map into echo image.")
        Echo("The image can be found at %appdata%/terrarum/Exports")
    }
}


