package net.torvald.terrarum.modulebasegame.console

import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isOre
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.serialise.toUint
import net.torvald.terrarum.utils.RasterWriter
import net.torvald.terrarum.worlddrawer.toRGBA
import java.io.File
import java.io.IOException
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Ground Peneration Radar Simulator
 *
 * Created by minjaesong on 2023-11-01.
 */
internal object ExportMap2 : ConsoleCommand {

    private fun getRCS(item: ItemID) =
        if (item.isOre())
            ItemCodex[OreCodex.oreProps.getOrElse(item) { null }?.item]?.material?.rcs ?: MaterialCodex.nullMaterial.rcs
        else
            MaterialCodex.getOrDefault(BlockCodex.getOrNull(item)?.material).rcs

    private fun triangularRand(amp: Float) = (((Math.random() + Math.random()) - 1.0) * amp).toFloat()

    private fun strToRandAmp(str: Float) = FastMath.log(str.absoluteValue.coerceAtLeast(4f), 2f).coerceAtLeast(0f)

    private fun Float.bfpow(pow: Float) = if (this >= 0f) this.div(128f).pow(pow) else this.div(-128f).pow(pow).times(-1f)

    private fun Float.toDitherredByte(): Byte {
        val byteVal = this.times(255f).roundToInt()
        val error = this - byteVal
        val errorInt = if (Math.random() < error.absoluteValue) 0 else error.sign.toInt()
        return (byteVal + errorInt).coerceIn(0..255).toByte()
    }

    private val kernPos = listOf(
        -1 to -1,
         0 to -1,
         1 to -1,
        -1 to  0,
         0 to  0,
         1 to  0,
        -1 to  1,
         0 to  1,
         1 to  1,
    )
    private val kernPow = listOf(
        1f / 16f,
        2f / 16f,
        1f / 16f,
        2f / 16f,
        4f / 16f,
        2f / 16f,
        1f / 16f,
        2f / 16f,
        1f / 16f
    )

    private val ECHO_DECAY = 2.4f
    private val ECHO_STRIDE = 4

    private fun avr(vararg fs: Float) = fs.sum() / fs.size

    private val gaussKernels = arrayOf(
        floatArrayOf(),
        floatArrayOf(1f),
        intArrayOf(2,1).map { it / 3f }.toFloatArray(),
        intArrayOf(6,4,1).map { it / 11f }.toFloatArray(),
        intArrayOf(20,15,6,1).map { it / 42f }.toFloatArray(),
        intArrayOf(70,56,28,2,1).map { it / 163f }.toFloatArray(),
    )

    private fun FloatArray.gaussianAvr(): Float {
        return this.zip(gaussKernels[this.size]).map { (f, w) -> f*w }.sum()
    }

    private fun Iterable<Float>.normaliseNaN(default: Float = 0f): FloatArray {
        return this.map { if (it.isNaN()) default else it }.toFloatArray()
    }

    override fun execute(args: Array<String>) {
        val world = (INGAME.world)
        val RAY = args.getOrNull(2)?.toFloat() ?: Float.POSITIVE_INFINITY
        if (args.size >= 2 && RAY != null) {

            // TODO rewrite to use Pixmap and PixmapIO

            val mapData = ByteArray(world.width * world.height) { 0x80.toByte() }
//            val mapDataFloat = FloatArray(world.width * world.height) { 0f }

            for (x in 0 until world.width) {
                val akku = floatArrayOf(0f,0f,0f,0f,0f)
                var energy = RAY

                // get values
                for (y in 0 until world.height) {
                    val reflection = if (energy > 0f) {
                        val terrs = kernPos.map { world.getTileFromTerrain(x + it.first, y + it.second) }
                        val ores = kernPos.map { world.getTileFromOre(x + it.first, y + it.second).item }


                        maxOf(kernPow.mapIndexed { index, mult ->
                            val reflection0 = maxOf(getRCS(terrs[index]), getRCS(ores[index]))
                            mult * (reflection0 + triangularRand(strToRandAmp(reflection0.toFloat())))
                        }.sum())
                    }
                    else {
                        0f
                    }


                    val delta = (reflection - akku[0]).coerceAtLeast(0f)
                    val delta2 = delta - akku[1]
                    val delta3 = delta2 - akku[2]
                    val delta4 = delta3 - akku[3]
                    val delta5 = delta4 - akku[4]


                    val deltaVal = delta5.bfpow(1f)
                    mapData[y * world.width + x] = deltaVal.plus(0.5f).toDitherredByte()
//                    mapDataFloat[y * world.width + x] = deltaVal
                    energy -= reflection

                    akku[4] = delta4
                    akku[3] = delta3
                    akku[2] = delta2
                    akku[1] = delta
                    akku[0] = reflection
                }

                // create artificial ringing artefacts
                for (y in ECHO_STRIDE until world.height) {
                    val srcOff = (y - ECHO_STRIDE) * world.width + x
                    val thisOff = y * world.width + x

                    val srcVal = mapData[srcOff].toUint().div(255f).minus(0.5f)
                    val backVal = mapData[thisOff].toUint().div(255f).minus(0.5f)
                    val newVal = backVal + srcVal / ECHO_DECAY

                    mapData[thisOff] = newVal.plus(0.5f).toDitherredByte()
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
