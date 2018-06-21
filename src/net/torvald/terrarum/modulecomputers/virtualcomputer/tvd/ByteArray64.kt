package net.torvald.terrarum.modulecomputers.virtualcomputer.tvd

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


/**
 * ByteArray that can hold larger than 2 GiB of Data.
 *
 * Works kind of like Bank Switching of old game console's cartridges which does same thing.
 *
 * Created by Minjaesong on 2017-04-12.
 */
class ByteArray64(val size: Long) {
    companion object {
        val bankSize: Int = 8192
    }

    private val data: Array<ByteArray>

    init {
        if (size < 0)
            throw IllegalArgumentException("Invalid array size!")

        val requiredBanks: Int = 1 + ((size - 1) / bankSize).toInt()

        data = Array<ByteArray>(
                requiredBanks,
                { bankIndex ->
                    kotlin.ByteArray(
                            if (bankIndex == requiredBanks - 1)
                                size.toBankOffset()
                            else
                                bankSize,

                            { 0.toByte() }
                    )
                }
        )
    }

    private fun Long.toBankNumber(): Int = (this / bankSize).toInt()
    private fun Long.toBankOffset(): Int = (this % bankSize).toInt()

    operator fun set(index: Long, value: Byte) {
        if (index < 0 || index >= size)
            throw ArrayIndexOutOfBoundsException("size $size, index $index")

        data[index.toBankNumber()][index.toBankOffset()] = value
    }

    operator fun get(index: Long): Byte {
        if (index < 0 || index >= size)
            throw ArrayIndexOutOfBoundsException("size $size, index $index")

        return data[index.toBankNumber()][index.toBankOffset()]
    }

    operator fun iterator(): ByteIterator {
        return object : ByteIterator() {
            var iterationCounter = 0L

            override fun nextByte(): Byte {
                iterationCounter += 1
                return this@ByteArray64[iterationCounter - 1]
            }

            override fun hasNext() = iterationCounter < this@ByteArray64.size
        }
    }

    fun iteratorChoppedToInt(): IntIterator {
        return object : IntIterator() {
            var iterationCounter = 0L
            val iteratorSize = 1 + ((this@ByteArray64.size - 1) / 4).toInt()

            override fun nextInt(): Int {
                var byteCounter = iterationCounter * 4L
                var int = 0
                (0..3).forEach {
                    if (byteCounter + it < this@ByteArray64.size) {
                        int += this@ByteArray64[byteCounter + it].toInt() shl (it * 8)
                    }
                    else {
                        int += 0 shl (it * 8)
                    }
                }


                iterationCounter += 1
                return int
            }

            override fun hasNext() = iterationCounter < iteratorSize
        }
    }

    fun forEach(consumer: (Byte) -> Unit) = iterator().forEach { consumer(it) }
    fun forEachInt32(consumer: (Int) -> Unit) = iteratorChoppedToInt().forEach { consumer(it) }
    fun forEachBanks(consumer: (ByteArray) -> Unit) = data.forEach(consumer)

    fun sliceArray64(range: LongRange): ByteArray64 {
        val newarr = ByteArray64(range.last - range.first + 1)
        range.forEach { index ->
            newarr[index - range.first] = this[index]
        }
        return newarr
    }

    fun sliceArray(range: IntRange): ByteArray {
        val newarr = ByteArray(range.last - range.first + 1)
        range.forEach { index ->
            newarr[index - range.first] = this[index.toLong()]
        }
        return newarr
    }

    fun toByteArray(): ByteArray {
        if (this.size > Integer.MAX_VALUE - 8) // according to OpenJDK; the size itself is VM-dependent
            throw TypeCastException("Impossible cast; too large to fit")

        return ByteArray(this.size.toInt(), { this[it.toLong()] })
    }

    fun writeToFile(file: File) {
        var fos = FileOutputStream(file, false)
        fos.write(data[0])
        fos.flush()
        fos.close()

        if (data.size > 1) {
            fos = FileOutputStream(file, true)
            for (i in 1..data.lastIndex) {
                fos.write(data[i])
                fos.flush()
            }
            fos.close()
        }
    }
}

class ByteArray64InputStream(val byteArray64: ByteArray64): InputStream() {
    private var readCounter = 0L

    override fun read(): Int {
        readCounter += 1

        return try {
            byteArray64[readCounter - 1].toUint()
        }
        catch (e: ArrayIndexOutOfBoundsException) {
            -1
        }
    }
}