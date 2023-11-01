package net.torvald.terrarum.modulebasegame.console

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.serialise.toUint
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.worlddrawer.toRGBA
import java.io.File
import java.io.IOException

/**
 * Ground Peneration Radar Simulator
 *
 * Created by minjaesong on 2023-11-01.
 */
internal object ExportMap2 : ConsoleCommand {

    //private var mapData: ByteArray? = null
    // private var mapDataPointer = 0


    private val oreColourMap = hashMapOf(
        Block.AIR to 0,
        "ores@basegame:1" to 160,
        "ores@basegame:2" to 160,
        "ores@basegame:3" to 160,
    )

    override fun execute(args: Array<String>) {
        val world = (INGAME.world)

        if (args.size == 2) {

            // TODO rewrite to use Pixmap and PixmapIO

            val mapData = ByteArray(world.width * world.height)

            for (x in 0 until world.width) {
                var akku = 0
                var akku2 = 0
                for (y in 0 until world.height) {
                    val terr = world.getTileFromTerrain(x, y)
                    val ore = world.getTileFromOre(x, y).item

                    val colOre = (oreColourMap.get(ore) ?: throw NullPointerException("nullore $ore"))
                    val colFore = (BlockCodex.getOrNull(terr)?.strength ?: throw NullPointerException("nullterr $terr"))
                    val reflection = maxOf(colOre, colFore)

                    val delta = reflection - akku
                    val delta2 = delta - akku2

                    mapData[y * world.width + x] = delta2.plus(128).coerceIn(0..255).toByte()

                    akku2 = delta
                    akku = reflection

                }
            }


            /*for (x in 0 until world.width) {
                var akku = 0
                for (y in 0 until world.height) {
                    val off = y * world.width + x

                    val reflection = mapData[off].toUint() - 128

                    val delta = reflection - akku
                    akku = reflection


                    mapData[off] = delta.plus(128).coerceIn(0..255).toByte()
                }
            }*/


            val dir = App.defaultDir + "/Exports/"
            val dirAsFile = File(dir)
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir()
            }

            try {
                RasterWriter.writePNG_Mono(
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
