package net.torvald.terrarum.tests

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.serialise.WriteLayerDataZip
import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.serialise.toULittle48
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * Created by minjaesong on 2019-02-22.
 */
object ByteArray64Test {

    val testOut1 = "TESTING ".toByteArray()
    val testOut2 = byteArrayOf(-1, -2)
    val testOut3 = """According to all known laws of aviation, there is no way a bee should be able to fly.
Its wings are too small to get its fat little body off the ground.
The bee, of course, flies anyway because bees don't care what humans think is impossible.
Yellow, black. Yellow, black. Yellow, black. Yellow, black.
Ooh, black and yellow! Let's shake it up a little.""".trimIndent().toByteArray()

    operator fun invoke() {
        val outputStream = ByteArray64GrowableOutputStream(16)

        fun wb(byteArray: ByteArray) { outputStream.write(byteArray) }
        fun wb(byte: Byte) { outputStream.write(byte.toInt()) }
        //fun wb(byte: Int) { outputStream.write(byte) }
        fun wi32(int: Int) { wb(int.toLittle()) }
        fun wi48(long: Long) { wb(long.toULittle48()) }
        fun wi64(long: Long) { wb(long.toLittle()) }
        fun wf32(float: Float) { wi32(float.toRawBits()) }



        wb(WriteLayerDataZip.MAGIC); wb(WriteLayerDataZip.VERSION_NUMBER); wb(WriteLayerDataZip.NUMBER_OF_LAYERS); wb(WriteLayerDataZip.NUMBER_OF_PAYLOADS); wb(WriteLayerDataZip.COMPRESSION_ALGORITHM); wb(WriteLayerDataZip.GENERATOR_VERSION)
        val deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)
        repeat(20001) {
            deflater.write(Math.random().times(256).toInt().and(255))
        }
        deflater.flush(); deflater.finish()
        wb(testOut2)


        outputStream.flush()
        outputStream.close()

        val osa = outputStream.toByteArray64()

        osa.forEach {
            print(it.toUInt().and(255u).toString(16).toUpperCase().padStart(2, '0'))
            print(' ')
        }
        println()

        osa.forEach {
            print(it.toChar())
        }
        println()
    }

}

fun main() {
    ByteArray64Test.invoke()
}