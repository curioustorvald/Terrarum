package net.torvald.terrarum.tests

import com.badlogic.gdx.utils.compression.Lzma
import io.airlift.compress.snappy.SnappyFramedInputStream
import io.airlift.compress.snappy.SnappyFramedOutputStream
import io.airlift.compress.zstd.ZstdInputStream
import io.airlift.compress.zstd.ZstdOutputStream
import net.torvald.random.HQRNG
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.ByteArray64
import net.torvald.terrarum.savegame.ByteArray64GrowableOutputStream
import net.torvald.terrarum.savegame.ByteArray64InputStream
import net.torvald.terrarum.serialise.toUint
import net.torvald.terrarum.toHex
import java.io.InputStream
import java.io.OutputStream
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
    private val testInputL = testInput0.copyOf().also { it.shuffle() }
    private val testInputZ = testInput0.copyOf().also { it.shuffle() }
    private val testInputS = testInput0.copyOf().also { it.shuffle() }

    private inline fun _comp(bytes: ByteArray64, zf: (ByteArray64GrowableOutputStream) -> OutputStream): ByteArray64 {
        val bo = ByteArray64GrowableOutputStream()
        val zo = zf(bo)

        bytes.iterator().forEach {
            zo.write(it.toInt())
        }
        zo.flush(); zo.close()
        return bo.toByteArray64()
    }
    private fun _decomp(bytes: ByteArray64, zf: (ByteArray64InputStream) -> InputStream): ByteArray64 {
        val unzipdBytes = ByteArray64()
        val zi = zf(ByteArray64InputStream(bytes))
        while (true) {
            val byte = zi.read()
            if (byte == -1) break
            unzipdBytes.appendByte(byte.toByte())
        }
        zi.close()
        return unzipdBytes
    }

    private fun compGzip(bytes: ByteArray64) =  _comp(bytes) { GZIPOutputStream(it) }
    private fun decompGzip(bytes: ByteArray64) = _decomp(bytes) { GZIPInputStream(it) }

    private fun compZstd(bytes: ByteArray64) =  _comp(bytes) { ZstdOutputStream(it) }
    private fun decompZstd(bytes: ByteArray64) = _decomp(bytes) { ZstdInputStream(it) }

    private fun compSnappy(bytes: ByteArray64) =  _comp(bytes) { SnappyFramedOutputStream(it) }
    private fun decompSnappy(bytes: ByteArray64) = _decomp(bytes) { SnappyFramedInputStream(it) }

    fun main() {
        val compBufG = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val compBufZ = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val compBufS = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val decompBufG = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val decompBufZ = arrayOfNulls<ByteArray64>(TEST_COUNT)
        val decompBufS = arrayOfNulls<ByteArray64>(TEST_COUNT)

        val gzipCompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                compBufG[i] = compGzip(testInputG[i])
            }
        }
        val gzipDecompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                decompBufG[i] = decompGzip(compBufG[i]!!)
            }
        }

        val zstdCompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                compBufZ[i] = compZstd(testInputZ[i])
            }
        }
        val zstdDecompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                decompBufZ[i] = decompZstd(compBufZ[i]!!)
            }
        }


        val snappyCompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                compBufS[i] = compSnappy(testInputS[i])
            }
        }
        val snappyDecompTime = measureNanoTime {
            for (i in 0 until TEST_COUNT) {
                decompBufS[i] = decompSnappy(compBufS[i]!!)
            }
        }


        val compSizeG = compBufG.sumOf { it!!.size } / TEST_COUNT
        val compSizeZ = compBufZ.sumOf { it!!.size } / TEST_COUNT
        val compSizeS = compBufS.sumOf { it!!.size } / TEST_COUNT
        val origSize =  testInput0.sumOf { it.size } / TEST_COUNT
        val ratioG = ((1.0 - (compSizeG.toDouble() / origSize)) * 10000).roundToInt() / 100
        val ratioZ = ((1.0 - (compSizeZ.toDouble() / origSize)) * 10000).roundToInt() / 100
        val ratioS = ((1.0 - (compSizeS.toDouble() / origSize)) * 10000).roundToInt() / 100

        println("==== $mode Data ($origSize bytes x $TEST_COUNT samples) ====")
        println("Gzip   comp: $gzipCompTime ns")
        println("Gzip decomp: $gzipDecompTime ns; ratio: $ratioG% (avr size: $compSizeG)")
        println("Zstd   comp: $zstdCompTime ns")
        println("Zstd decomp: $zstdDecompTime ns; ratio: $ratioZ% (avr size: $compSizeZ)")
        println("Snpy   comp: $snappyCompTime ns")
        println("Snpy decomp: $snappyDecompTime ns; ratio: $ratioS% (avr size: $compSizeS)")
        println()

        repeat(2) { sg.add(compBufG.random()!!.sliceArray(0..15).joinToString { it.toUint().toHex().takeLast(2) }) }
        repeat(2) { sz.add(compBufZ.random()!!.sliceArray(0..15).joinToString { it.toUint().toHex().takeLast(2) }) }
        repeat(2) { ss.add(compBufS.random()!!.sliceArray(0..15).joinToString { it.toUint().toHex().takeLast(2) }) }



    }
}

private val sg = ArrayList<String>()
private val sz = ArrayList<String>()
private val ss = ArrayList<String>()

fun main() {
    ZipTest("Simulated Real-World").main()
    ZipTest("Zero-Filled").main()
    ZipTest("Random").main()

    println("Gzip samples:")
    sg.forEach { println(it) }
    println("Zstd samples:")
    sz.forEach { println(it) }
    println("Snappy samples:")
    ss.forEach { println(it) }
}