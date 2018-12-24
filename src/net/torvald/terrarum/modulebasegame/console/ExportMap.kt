package net.torvald.terrarum.modulebasegame.console

import net.torvald.colourutil.Col4096
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError

import java.io.*
import java.util.HashMap

/**
 * Created by minjaesong on 2016-01-17.
 */
internal object ExportMap : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0

    private val colorTable = HashMap<Int, Col4096>()

    init {
        colorTable.put(Block.AIR, Col4096(0xCEF))
        colorTable.put(Block.STONE, Col4096(0x888))
        colorTable.put(Block.DIRT, Col4096(0x753))
        colorTable.put(Block.GRASS, Col4096(0x472))

        colorTable.put(Block.ORE_COPPER, Col4096(0x6A8))
        colorTable.put(Block.ORE_IRON, Col4096(0xC75))
        colorTable.put(Block.ORE_GOLD, Col4096(0xA87))
        colorTable.put(Block.ORE_ILMENITE, Col4096(0x8AB))
        colorTable.put(Block.ORE_AURICHALCUM, Col4096(0xD92))
        colorTable.put(Block.ORE_SILVER, Col4096(0xDDD))

        colorTable.put(Block.RAW_DIAMOND, Col4096(0x2BF))
        colorTable.put(Block.RAW_RUBY, Col4096(0xB10))
        colorTable.put(Block.RAW_EMERALD, Col4096(0x0B1))
        colorTable.put(Block.RAW_SAPPHIRE, Col4096(0x01B))
        colorTable.put(Block.RAW_TOPAZ, Col4096(0xC70))
        colorTable.put(Block.RAW_AMETHYST, Col4096(0x70C))

        colorTable.put(Block.WATER, Col4096(0x038))

        colorTable.put(Block.SAND, Col4096(0xDDB))
        colorTable.put(Block.SAND_WHITE, Col4096(0xFFD))
        colorTable.put(Block.SAND_RED, Col4096(0xA32))
        colorTable.put(Block.SAND_DESERT, Col4096(0xEDB))
        colorTable.put(Block.SAND_BLACK, Col4096(0x444))
        colorTable.put(Block.SAND_GREEN, Col4096(0x9A6))

        colorTable.put(Block.GRAVEL, Col4096(0x664))
        colorTable.put(Block.GRAVEL_GREY, Col4096(0x999))

        colorTable.put(Block.ICE_NATURAL, Col4096(0x9AB))
        colorTable.put(Block.ICE_MAGICAL, Col4096(0x7AC))
        colorTable.put(Block.ICE_FRAGILE, Col4096(0x6AF))
        colorTable.put(Block.SNOW, Col4096(0xCDE))
    }

    override fun execute(args: Array<String>) {
        val world = (Terrarum.ingame!!.world)
        
        if (args.size == 2) {

            var mapData = ByteArray(world.width * world.height * 3)
            var mapDataPointer = 0

            for (tile in world.terrainIterator()) {
                val colArray = (colorTable as Map<Int, Col4096>)
                        .getOrElse(tile, { Col4096(0xFFF) }).toByteArray()

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

    override fun printUsage() {

        Echo("Usage: export <name>")
        Echo("Exports current map into echo image.")
        Echo("The image can be found at %appdata%/terrarum/Exports")
    }
}


