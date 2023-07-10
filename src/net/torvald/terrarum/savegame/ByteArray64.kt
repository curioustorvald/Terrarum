package net.torvald.terrarum.savegame

import net.torvald.terrarum.serialise.toUint
import java.io.*
import java.nio.channels.ClosedChannelException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import kotlin.math.max
import kotlin.math.min


/**
 * ByteArray that can hold larger than 2 GiB of Data.
 *
 * Works kind of like Bank Switching of old game console's cartridges which does same thing.
 *
 * Note that this class is just a fancy ArrayList. Internal size will grow accordingly
 *
 * @param initialSize Initial size of the array. If it's not specified, 8192 will be used instead.
 *
 * Created by Minjaesong on 2017-04-12.
 */
class ByteArray64(initialSize: Long = BANK_SIZE.toLong()) {
    var internalCapacity: Long = initialSize
        private set

    var size = 0L
        internal set

    private var finalised = false

    companion object {
        val BANK_SIZE: Int = 8192

        fun fromByteArray(byteArray: ByteArray): ByteArray64 {
            val ba64 = ByteArray64(byteArray.size.toLong())
//            byteArray.forEachIndexed { i, byte -> ba64[i.toLong()] = byte }
            ba64.appendBytes(byteArray)
            return ba64
        }
    }

    private val __data: ArrayList<ByteArray>

    private fun checkMutability() {
        if (finalised) throw IllegalStateException("ByteArray64 is finalised and cannot be modified")
    }

    init {
        if (internalCapacity < 0)
            throw IllegalArgumentException("Invalid array size: $internalCapacity")
        else if (internalCapacity == 0L) // signalling empty array
            internalCapacity = BANK_SIZE.toLong()

        val requiredBanks: Int = (initialSize - 1).toBankNumber() + 1

        __data = ArrayList<ByteArray>(requiredBanks)
        repeat(requiredBanks) { __data.add(ByteArray(BANK_SIZE)) }
    }

    private fun Long.toBankNumber(): Int = (this / BANK_SIZE).toInt()
    private fun Long.toBankOffset(): Int = (this % BANK_SIZE).toInt()

    operator fun set(index: Long, value: Byte) {
        checkMutability()
        ensureCapacity(index + 1)

        try {
            __data[index.toBankNumber()][index.toBankOffset()] = value
            size = max(size, index + 1)
        }
        catch (e: IndexOutOfBoundsException) {
            val msg = "index: $index -> bank ${index.toBankNumber()} offset ${index.toBankOffset()}\n" +
                    "But the array only contains ${__data.size} banks.\n" +
                    "InternalCapacity = $internalCapacity, Size = $size"
            throw IndexOutOfBoundsException(msg)
        }
    }

    fun appendByte(value: Byte) = set(size, value)

    fun appendBytes(bytes: ByteArray64) {
        checkMutability()
        ensureCapacity(size + bytes.size)

        val bankOffset = size.toBankOffset()
        val initialBankNumber = size.toBankNumber()
        val remaining = BANK_SIZE - bankOffset

        bytes.forEachUsedBanksIndexed { index, bytesInBank, srcBank ->
            // as the data must be written bank-aligned, each bank copy requires two separate copies, split by the
            // 'remaining' below
            if (remaining < bytesInBank) { // 'remaining' should never be less than zero
                System.arraycopy(srcBank, 0, __data[initialBankNumber + index], bankOffset, remaining)
                System.arraycopy(srcBank, remaining, __data[initialBankNumber + index + 1], 0, bytesInBank - remaining)
            }
            else if (bytesInBank > 0) {
                System.arraycopy(srcBank, 0, __data[initialBankNumber + index], bankOffset, bytesInBank)
            }
        }

        size += bytes.size
    }

    fun appendBytes(bytes: ByteArray) {
        checkMutability()
        ensureCapacity(size + bytes.size)
        val bankOffset = size.toBankOffset()
        var currentBankNumber = size.toBankNumber()
        val remainingInHeadBank = BANK_SIZE - bankOffset // how much space left in the current (= head) bank
        var remainingBytesToCopy = bytes.size
        var srcCursor = 0

        // as the source is single contiguous byte array, we only need three separate copies:
        // 1. Copy over some bytes so that the current bank is fully filled
        // 2. Copy over 8192*n bytes to fill a chunk in single operation
        // 3. Copy over the remaining bytes

        // 1.
        var actualBytesToCopy = min(remainingBytesToCopy, remainingInHeadBank) // it is possible that size of the bytes is smaller than the remainingInHeadBank
        System.arraycopy(bytes, srcCursor, __data[currentBankNumber], bankOffset, actualBytesToCopy)
        remainingBytesToCopy -= actualBytesToCopy
        srcCursor += actualBytesToCopy
        if (remainingBytesToCopy <= 0) { size += bytes.size; return }

        // 2. and 3.
        while (remainingBytesToCopy > 0) {
            currentBankNumber += 1
            actualBytesToCopy = min(remainingBytesToCopy, BANK_SIZE) // it is possible that size of the bytes is smaller than the remainingInHeadBank
            System.arraycopy(bytes, srcCursor, __data[currentBankNumber], 0, actualBytesToCopy)
            remainingBytesToCopy -= actualBytesToCopy
            srcCursor += actualBytesToCopy
        }
        size += bytes.size; return
    }

    operator fun get(index: Long): Byte {
        if (index < 0 || index >= size)
            throw ArrayIndexOutOfBoundsException("size $size, index $index")

        try {
            val r = __data[index.toBankNumber()][index.toBankOffset()]
            return  r
        }
        catch (e: IndexOutOfBoundsException) {
            System.err.println("index: $index -> bank ${index.toBankNumber()} offset ${index.toBankOffset()}")
            System.err.println("But the array only contains ${__data.size} banks.")

            throw e
        }
    }

    private fun addOneBank() {
        __data.add(ByteArray(BANK_SIZE))
        internalCapacity = __data.size * BANK_SIZE.toLong()
    }

    /**
     * Increases the capacity of it, if necessary, to ensure that it can hold at least the number of elements specified by the minimum capacity argument.
     */
    fun ensureCapacity(minCapacity: Long) {
        while (minCapacity > internalCapacity) {
            addOneBank()
        }
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

    /** Iterates over all written bytes. */
    fun forEach(consumer: (Byte) -> Unit) = iterator().forEach { consumer(it) }
    /** Iterates over all written 32-bit words. */
    fun forEachInt32(consumer: (Int) -> Unit) = iteratorChoppedToInt().forEach { consumer(it) }
    /** Iterates over all existing banks, even if they are not used. Please use [forEachUsedBanks] to iterate over banks that are actually been used. */
    fun forEachBanks(consumer: (ByteArray) -> Unit) = __data.forEach(consumer)
    /** Iterates over all written bytes. */
    fun forEachIndexed(consumer: (Long, Byte) -> Unit) {
        var cnt = 0L
        iterator().forEach {
            consumer(cnt, it)
            cnt += 1
        }
    }
    /** Iterates over all written 32-bit words. */
    fun forEachInt32Indexed(consumer: (Long, Int) -> Unit) {
        var cnt = 0L
        iteratorChoppedToInt().forEach {
            consumer(cnt, it)
            cnt += 1
        }
    }

    /**
     * @param consumer (Int, Int, ByteArray)-to-Unit function where first Int is index;
     * second Int is actual number of bytes written in that bank, 0..BANK_SIZE (0 means that the bank is unused)
     */
    fun forEachUsedBanksIndexed(consumer: (Int, Int, ByteArray) -> Unit) {
        __data.forEachIndexed { index, bytes ->
            consumer(index, (size - BANK_SIZE * index).coerceIn(0, BANK_SIZE.toLong()).toInt(), bytes)
        }
    }

    /**
     * @param consumer (Int, ByteArray)-to-Unit function where Int is actual number of bytes written in that bank, 0..BANK_SIZE (0 means that the bank is unused)
     */
    fun forEachUsedBanks(consumer: (Int, ByteArray) -> Unit) {
        __data.forEachIndexed { index, bytes ->
            consumer((size - BANK_SIZE * index).coerceIn(0, BANK_SIZE.toLong()).toInt(), bytes)
        }
    }

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

        return ByteArray(this.size.toInt()) { this[it.toLong()] }
    }

    fun writeToFile(file: File) {
        var fos = FileOutputStream(file, false)
        // following code writes in-chunk basis
        /*fos.write(__data[0])
        fos.flush()
        fos.close()

        if (__data.size > 1) {
            fos = FileOutputStream(file, true)
            for (i in 1..__data.lastIndex) {
                fos.write(__data[i])
                fos.flush()
            }
            fos.close()
        }*/

        forEach {
            fos.write(it.toInt())
        }
        fos.flush()
        fos.close()
    }

    fun finalise() {
        this.finalised = true
    }
}

open class ByteArray64InputStream(val byteArray64: ByteArray64): InputStream() {
    protected open var readCounter = 0L

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

/** Static ByteArray OutputStream. Less leeway, more stable. */
open class ByteArray64OutputStream(val byteArray64: ByteArray64): OutputStream() {
    protected open var writeCounter = 0L

    override fun write(b: Int) {
        try {
            byteArray64.appendByte(b.toByte())
            writeCounter += 1
        }
        catch (e: ArrayIndexOutOfBoundsException) {
            throw IOException(e)
        }
    }

    override fun close() {
        byteArray64.finalise()
    }
}

/** Just like Java's ByteArrayOutputStream, except its size grows if you exceed the initial size
 */
open class ByteArray64GrowableOutputStream(size: Long = ByteArray64.BANK_SIZE.toLong()): OutputStream() {
    protected open var buf = ByteArray64(size)
    protected open var count = 0L

    private var finalised = false

    init {
        if (size <= 0L) throw IllegalArgumentException("Illegal array size: $size")
    }

    override fun write(b: Int) {
        if (finalised) {
            throw IllegalStateException("This output stream is finalised and cannot be modified.")
        }
        else {
            buf.appendByte(b.toByte())
            count += 1
        }
    }

    /** Unlike Java's, this does NOT create a copy of the internal buffer; this just returns its internal.
     * This method also "finalises" the buffer inside of the output stream, making further modification impossible.
     *
     * The output stream must be flushed and closed, warning you of closing the stream is not possible.
     */
    @Synchronized
    fun toByteArray64(): ByteArray64 {
        close()
        buf.size = count
        return buf
    }

    override fun close() {
        finalised = true
        buf.finalise()
    }
}

open class ByteArray64Writer(val charset: Charset) : Writer() {

    /* writer must be able to handle nonstandard utf-8 surrogate representation, where
     * each surrogate is encoded in single code point, resulting six utf-8 bytes instead of four.
     */

    private val acceptableCharsets = arrayOf(Charsets.UTF_8, Charset.forName("CP437"))

    init {
        if (!acceptableCharsets.contains(charset))
            throw UnsupportedCharsetException(charset.name())
    }

    private val ba = ByteArray64()
    private var closed = false
    private var surrogateBuf = 0

    init {
        this.lock = ba
    }

    private fun checkOpen() {
        if (closed) throw ClosedChannelException()
    }

    private fun Int.isSurroHigh() = this.ushr(10) == 0b110110
    private fun Int.isSurroLow() = this.ushr(10) == 0b110111
    private fun Int.toUcode() = 'u' + this.toString(16).toUpperCase().padStart(4,'0')

    /**
     * @param c not a freakin' codepoint; just a Java's Char casted into Int
     */
    override fun write(c: Int) {
        checkOpen()
        when (charset) {
            Charsets.UTF_8 -> {
                if (surrogateBuf == 0 && !c.isSurroHigh() && !c.isSurroLow())
                    writeUtf8Codepoint(c)
                else if (surrogateBuf == 0 && c.isSurroHigh())
                    surrogateBuf = c
                else if (surrogateBuf != 0 && c.isSurroLow())
                    writeUtf8Codepoint(65536 + surrogateBuf.and(1023).shl(10) or c.and(1023))
                // invalid surrogate pair input
                else
                    throw IllegalStateException("Surrogate high: ${surrogateBuf.toUcode()}, surrogate low: ${c.toUcode()}")
            }
            Charset.forName("CP437") -> {
                ba.appendByte(c.toByte())
            }
            else -> throw UnsupportedCharsetException(charset.name())
        }
    }

    fun writeUtf8Codepoint(codepoint: Int) {
        when (codepoint) {
            in 0..127 -> ba.appendByte(codepoint.toByte())
            in 128..2047 -> {
                ba.appendByte((0xC0 or codepoint.ushr(6).and(31)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            in 2048..65535 -> {
                ba.appendByte((0xE0 or codepoint.ushr(12).and(15)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            in 65536..1114111 -> {
                ba.appendByte((0xF0 or codepoint.ushr(18).and(7)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(12).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            else -> throw IllegalArgumentException("Not a unicode code point: U+${codepoint.toString(16).toUpperCase()}")
        }
    }

    override fun write(cbuf: CharArray) {
        checkOpen()
        write(String(cbuf))
    }

    override fun write(str: String) {
        checkOpen()
        ba.appendBytes(str.toByteArray(charset))
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        write(cbuf.copyOfRange(off, off + len))
    }

    override fun write(str: String, off: Int, len: Int) {
        write(str.substring(off, off + len))
    }

    override fun close() { closed = true }
    override fun flush() {}

    fun toByteArray64() = if (closed) ba else throw IllegalAccessException("Writer not closed")
}

open class ByteArray64Reader(val ba: ByteArray64, val charset: Charset) : Reader() {

    /* reader must be able to handle nonstandard utf-8 surrogate representation, where
     * each surrogate is encoded in single code point, resulting six utf-8 bytes instead of four.
     */

    private val acceptableCharsets = arrayOf(Charsets.UTF_8, Charset.forName("CP437"))

    init {
        if (!acceptableCharsets.contains(charset))
            throw UnsupportedCharsetException(charset.name())
    }

    private var readCursor = 0L
    private val remaining
        get() = ba.size - readCursor

    /**
     *   U+0000 .. U+007F 	0xxxxxxx
     *   U+0080 .. U+07FF 	110xxxxx 	10xxxxxx
     *   U+0800 .. U+FFFF 	1110xxxx 	10xxxxxx 	10xxxxxx
     * U+10000 .. U+10FFFF 	11110xxx 	10xxxxxx 	10xxxxxx 	10xxxxxx
     */
    private fun utf8GetCharLen(head: Byte) = when (head.toInt() and 255) {
        in 0b11110_000..0b11110_111 -> 4
        in 0b1110_0000..0b1110_1111 -> 3
        in 0b110_00000..0b110_11111 -> 2
        in 0b0_0000000..0b0_1111111 -> 1
        else -> throw IllegalArgumentException("Invalid UTF-8 Character head byte: ${head.toInt() and 255}")
    }

    /**
     * @param list of bytes that encodes one unicode character. Get required byte length using [utf8GetCharLen].
     * @return A codepoint of the character.
     */
    private fun utf8decode(bytes0: List<Byte>): Int {
        val bytes = bytes0.map { it.toInt() and 255 }
        var ret = when (bytes.size) {
            4 -> (bytes[0] and 7) shl 18
            3 -> (bytes[0] and 15) shl 12
            2 -> (bytes[0] and 31) shl 6
            1 -> (bytes[0] and 127)
            else -> throw IllegalArgumentException("Expected bytes size: 1..4, got ${bytes.size}")
        }
        bytes.subList(1, bytes.size).reversed().forEachIndexed { index, byte ->
            ret = ret or (byte and 63).shl(6 * index)
        }
        return ret
    }

    private var surrogateLeftover = ' '

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        var readCount = 0

        if (remaining <= 0L) return -1

        when (charset) {
            Charsets.UTF_8 -> {
                while (readCount < len && remaining > 0) {
                    if (surrogateLeftover != ' ') {
                        cbuf[off + readCount] = surrogateLeftover

                        readCount += 1
                        surrogateLeftover = ' '
                    }
                    else {
                        val bbuf = (0 until min(4L, remaining)).map { ba[readCursor + it] }
                        val charLen = utf8GetCharLen(bbuf[0])
                        val codePoint = utf8decode(bbuf.subList(0, charLen))

                        if (codePoint < 65536) {
                            cbuf[off + readCount] = codePoint.toChar()

                            readCount += 1
                            readCursor += charLen
                        }
                        else {
                            /*
                         * U' = yyyyyyyyyyxxxxxxxxxx  // U - 0x10000
                         * W1 = 110110yyyyyyyyyy      // 0xD800 + yyyyyyyyyy
                         * W2 = 110111xxxxxxxxxx      // 0xDC00 + xxxxxxxxxx
                         */
                            val codPoin = codePoint - 65536
                            val surroLead = (0xD800 or codPoin.ushr(10)).toChar()
                            val surroTrail = (0xDC00 or codPoin.and(1023)).toChar()

                            cbuf[off + readCount] = surroLead

                            if (off + readCount + 1 < cbuf.size) {
                                cbuf[off + readCount + 1] = surroTrail

                                readCount += 2
                                readCursor += 4
                            }
                            else {
                                readCount += 1
                                readCursor += 4
                                surrogateLeftover = surroTrail
                            }
                        }
                    }
                }
            }
            Charset.forName("CP437") -> {
                for (i in 0 until min(len.toLong(), remaining)) {
                    cbuf[(off + i).toInt()] = ba[readCursor].toChar()
                    readCursor += 1
                    readCount += 1
                }
            }
            else -> throw UnsupportedCharsetException(charset.name())
        }

        return readCount
    }

    override fun close() { readCursor = 0L }
    override fun reset() { readCursor = 0L }
    override fun markSupported() = false

}