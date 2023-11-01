package net.torvald.terrarum.modulebasegame.console

import com.jme3.math.FastMath
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
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

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
        "ores@basegame:2" to 128,
        "ores@basegame:3" to 96,
    )

    private fun triangularRand(amp: Float) = (((Math.random() + Math.random()) - 1.0) * amp).toFloat()

    private fun strToRandAmp(str: Float) = FastMath.log(str.absoluteValue.coerceAtLeast(4f), 2f).coerceAtLeast(0f)

    private fun Float.bfpow(pow: Float) = if (this >= 0f) this.div(128f).pow(pow) else this.div(-128f).pow(pow).times(-1f)


    override fun execute(args: Array<String>) {
        val world = (INGAME.world)
        if (args.size == 2) {

            // TODO rewrite to use Pixmap and PixmapIO

            val mapData = ByteArray(world.width * world.height)

            for (x in 0 until world.width) {
                var akku  = 0f
                var akku2 = 0f
                var akku3 = 0f
                var akku4 = 0f
                var akku5 = 0f
                for (y in 0 until world.height) {
                    val terr = world.getTileFromTerrain(x, y)
                    val ore = world.getTileFromOre(x, y).item

                    val colOre = (oreColourMap.get(ore) ?: throw NullPointerException("nullore $ore"))
                    val colFore = (BlockCodex.getOrNull(terr)?.strength ?: throw NullPointerException("nullterr $terr"))
                    val reflection0 = maxOf(colOre, colFore)
                    val reflection = reflection0 + triangularRand(strToRandAmp(reflection0.toFloat()))

                    val delta = (reflection - akku).coerceAtLeast(0f)
                    val delta2 = delta - akku2
                    val delta3 = delta2 - akku3
                    val delta4 = delta3 - akku4
                    val delta5 = delta4 - akku5


                    val deltaVal = delta5.bfpow(1f)
                    mapData[y * world.width + x] = deltaVal.plus(0.5f).times(255f).coerceIn(0f..255f).roundToInt().toByte()


                    akku5 = delta4
                    akku4 = delta3
                    akku3 = delta2
                    akku2 = delta
                    akku = reflection

                }
            }


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
