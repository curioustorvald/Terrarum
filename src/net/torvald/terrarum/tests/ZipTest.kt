package net.torvald.terrarum.tests

import io.airlift.compress.zstd.ZstdInputStream
import io.airlift.compress.zstd.ZstdOutputStream
import net.torvald.random.HQRNG
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.ByteArray64
import net.torvald.terrarum.savegame.ByteArray64GrowableOutputStream
import net.torvald.terrarum.savegame.ByteArray64InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

/**
 * Created by minjaesong on 2023-12-20.
 */
class ZipTest(val mode: String) {

    val rnd = HQRNG()

    private val generateRLErandData = { size: Int ->
        val r = ByteArray64()
        var c = 0
        var payloadSize = 0
        var currentPayload1 = 0.toByte()
        var currentPayload2 = 0.toByte()
        var tiktok = 0
        while (c < size) {
            if (payloadSize == 0) {
                payloadSize = rnd.nextInt(1, 64) * 2
                currentPayload1 = rnd.nextInt(0, 256).toByte()
                currentPayload2 = rnd.nextInt(0, 256).toByte()
            }

            if (tiktok == 0)
                r.appendByte(currentPayload1)
            else
                r.appendByte(currentPayload2)

            c++
            payloadSize--
            tiktok = 1 - tiktok
        }
        r
    }

    private val generateZerofilled = { size: Int ->
        val r = ByteArray64()
        val zero = 0.toByte()
        for (i in 0 until size) r.appendByte(zero)
        r
    }

    private val generateFullRandom = { size: Int ->
        val r = ByteArray64()
        for (i in 0 until size) r.appendByte(rnd.nextInt(0, 256).toByte())
        r
    }

    val dataGenerator = when (mode) {
        "Simulated Real-World" -> generateRLErandData
        "Zero-Filled" -> generateZerofilled
        "Random" -> generateFullRandom
        else -> throw IllegalArgumentException()
    }

    private val CHUNKSIZE = CHUNK_W * CHUNK_H
    private val TEST_COUNT = 5000

    private val testInput0 = Array(TEST_COUNT) { dataGenerator(CHUNKSIZE) }
    private val testInputG = testInput0.copyOf().also { it.shuffle() }
    private val testInputZ = testInput0.copyOf().also { it.shuffle() }

    private fun compGzip(bytes: ByteArray64): ByteArray64 {
        val bo = ByteArray64GrowableOutputStream()
        val zo = GZIPOutputStream(bo)

        bytes.iterator().forEach {
            zo.write(it.toInt())
        }
        zo.flush(); zo.close()
        return bo.toByteArray64()
    }

    private fun decompGzip(bytes: ByteArray64): ByteArray64 {
        val unzipdBytes = ByteArray64()
        val zi = GZIPInputStream(ByteArray64InputStream(bytes))
        while (true) {
            val byte = zi.read()
            if (byte == -1) break
            unzipdBytes.appendByte(byte.toByte())
        }
        zi.close()
        return unzipdBytes
    }

    private fun compZstd(bytes: ByteArray64): ByteArray64 {
        val bo = ByteArray64GrowableOutputStream()
        val zo = ZstdOutputStream(bo)

        bytes.iterator().forEach {
            zo.write(it.toInt())
        }
        zo.flush();zo.close()
        return bo.toByteArray64()
    }

    private fun decompZstd(bytes: ByteArray64): ByteArray64 {
        val unzipdBytes = ByteArray64()
        val zi = ZstdInputStream(ByteArray64InputStream(bytes))
        while (true) {
            val byte = zi.read()
            if (byte == -1) break
            unzipdBytes.appendByte(byte.toByte())
        }
        zi.close()
        return unzipdBytes
    }

    fun main() {
        val compBufG = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val compBufZ = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val decompBufG = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val decompBufZ = arrayOfNulls<ByteArray64>(TEST_COUNT)

//        println("Compressing $TEST_COUNT samples of $CHUNKSIZE bytes using Gzip")
        val gzipCompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                compBufG[i] = compGzip(testInputG[i])
            }
        }

//        println("Decompressing $TEST_COUNT samples of $CHUNKSIZE bytes using Gzip")
        val gzipDecompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                decompBufG[i] = decompGzip(compBufG[i]!!)
            }
        }

//        println("Compressing $TEST_COUNT samples of $CHUNKSIZE bytes using Zstd")
        val zstdCompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                compBufZ[i] = compZstd(testInputZ[i])
            }
        }

//        println("Decompressing $TEST_COUNT samples of $CHUNKSIZE bytes using Zstd")
        val zstdDecompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                decompBufZ[i] = decompZstd(compBufZ[i]!!)
            }
        }

        val compSizeG = compBufG.sumOf { it!!.size } / TEST_COUNT
        val compSizeZ = compBufZ.sumOf { it!!.size } / TEST_COUNT
        val origSize =  testInput0.sumOf { it.size } / TEST_COUNT
        val ratioG = ((1.0 - (compSizeG.toDouble() / origSize)) * 10000).roundToInt() / 100
        val ratioZ = ((1.0 - (compSizeZ.toDouble() / origSize)) * 10000).roundToInt() / 100

        println("==== $mode Data ($origSize bytes x $TEST_COUNT samples) ====")
        println("Gzip   comp: $gzipCompTime ns")
        println("Gzip decomp: $gzipDecompTime ns; ratio: $ratioG% (avr size: $compSizeG)")
        println("Zstd   comp: $zstdCompTime ns")
        println("Zstd decomp: $zstdDecompTime ns; ratio: $ratioZ% (avr size: $compSizeZ)")
        println()
    }
}


fun main() {
    ZipTest("Simulated Real-World").main()
    ZipTest("Zero-Filled").main()
    ZipTest("Random").main()
}