package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.graphics.Color
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.Companion.WALL_OVERLAY_COLOUR
import net.torvald.terrarum.worlddrawer.toRGBA
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2016-01-17.
 */
internal object ExportMap : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0


    private val oreColourMap = hashMapOf(
        Block.AIR to Cvec(0),
        "ores@basegame:1" to Cvec(0x00e9c8ff),
        "ores@basegame:2" to Cvec(0xff7e74ff.toInt()),
        "ores@basegame:3" to Cvec(0x383314ff),
        "ores@basegame:4" to Cvec(0xefde76ff.toInt()),
        "ores@basegame:5" to Cvec(0xcd8b62ff.toInt()),
        "ores@basegame:6" to Cvec(0xffcc00ff.toInt()),
        "ores@basegame:7" to Cvec(0xd5d9f9ff.toInt()),
        "ores@basegame:8" to Cvec(0xff9300ff.toInt()),
    )

    private val WALL_OVERLAY = Cvec(0.35f, 0.35f, 0.35f, 1f)

    override fun execute(args: Array<String>) {
        val world = (INGAME.world)
        
        if (args.size == 2) {

            // TODO rewrite to use Pixmap and PixmapIO

            val mapData = ByteArray(world.width * world.height * 3)
            var mapDataPointer = 0

            for ((terr, wall, ore) in world.terrainIterator()) {
                val colOre = (oreColourMap.get(ore) ?: throw NullPointerException("nullore $ore")).toByteArray()
                val colFore = (App.tileMaker.terrainTileColourMap.get(terr) ?: throw NullPointerException("nullterr $terr")).toByteArray()
                val colWall = (App.tileMaker.terrainTileColourMap.get(wall) ?: throw NullPointerException("nullwall $wall")).cpy().mul(WALL_OVERLAY).toByteArray()

                val terrProp = BlockCodex[terr]

                val colArray = if (ore != Block.AIR) colOre else if (terrProp.isSolid || terrProp.hasTag("TREE")) colFore else colWall


                for (i in 0..2) {
                    mapData[mapDataPointer + i] = colArray[i]
                }

                mapDataPointer += 3
            }

            val dir = App.defaultDir + "/Exports/"
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


