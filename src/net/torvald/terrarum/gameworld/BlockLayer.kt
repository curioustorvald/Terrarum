package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import net.torvald.UnsafeHelper
import net.torvald.terrarum.AppLoader.printdbg

/**
 * Original version Created by minjaesong on 2016-01-17.
 * Unsafe version Created by minjaesong on 2019-06-08.
 *
 * Note to self: refrain from using shorts--just do away with two bytes: different system have different endianness
 */
open class BlockLayer(val width: Int, val height: Int) : Disposable {

    // using unsafe pointer gets you 100 fps, whereas using directbytebuffer gets you 90
    private val ptr = UnsafeHelper.allocate(width * height * BYTES_PER_BLOCK)

    //private val directByteBuffer: ByteBuffer

    init {
        ptr.fillWith(0)

        //directByteBuffer = ByteBuffer.allocateDirect(width * height * BYTES_PER_BLOCK)
    }

    /**
     * @param data Byte array representation of the layer, where:
     * - every 2n-th byte is lowermost 8 bits of the tile number
     * - every (2n+1)th byte is uppermost 4 (4096 blocks) or 8 (65536 blocks) bits of the tile number.
     *
     * When 4096-block mode is being used, every (2n+1)th byte is filled in this format:
     * ```
     * (MSB) 0 0 0 0 a b c d (LSB)
     * ```
     *
     * In other words, the valid range for the every (2n+1)th byte is 0..15.
     *
     * TL;DR: LITTLE ENDIAN PLEASE
     */
    constructor(width: Int, height: Int, data: ByteArray) : this(width, height) {
        TODO()
        //data.forEachIndexed { index, byte -> unsafe.putByte(layerPtr + index, byte) }
    }


    /**
     * Returns an iterator over blocks of type `Int`.
     *
     * @return an Iterator.
     */
    fun blocksIterator(): Iterator<Int> {
        return object : Iterator<Int> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): Int {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 2

                val offset = 2L * (y * width + x)
                val lsb = ptr[offset]
                //val lsb = directByteBuffer[offset]
                val msb = ptr[offset + 1]
                //val msb = directByteBuffer[offset + 1]


                return lsb.toUint() + msb.toUint().shl(8)
            }
        }
    }

    /**
     * Returns an iterator over stored bytes.
     *
     * @return an Iterator.
     */
    fun bytesIterator(): Iterator<Byte> {
        return object : Iterator<Byte> {

            private var iteratorCount = 0L

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): Byte {
                iteratorCount += 1

                return ptr[iteratorCount]
                //return directByteBuffer[iteratorCount]
            }
        }
    }

    internal fun unsafeGetTile(x: Int, y: Int): Int {
        val offset = BYTES_PER_BLOCK * (y * width + x)
        val lsb = ptr[offset]
        val msb = ptr[offset + 1]
        //val lsb = directByteBuffer[offset]
        //val msb = directByteBuffer[offset + 1]

        return lsb.toUint() + msb.toUint().shl(8)
    }

    internal fun unsafeSetTile(x: Int, y: Int, tile: Int) {
        val offset = BYTES_PER_BLOCK * (y * width + x)

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()


        ptr[offset] = lsb
        ptr[offset + 1] = msb
        //directByteBuffer.put(offset, lsb)
        //directByteBuffer.put(offset + 1, msb)
    }

    /**
     * @param blockOffset Offset in blocks. BlockOffset of 0x100 is equal to ```layerPtr + 0x200```
     */
    /*internal fun unsafeSetTile(blockOffset: Long, tile: Int) {
        val offset = BYTES_PER_BLOCK * blockOffset

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()

        unsafe.putByte(layerPtr + offset, lsb)
        unsafe.putByte(layerPtr + offset + 1, msb)
    }*/

    fun isInBound(x: Int, y: Int) = (x >= 0 && y >= 0 && x < width && y < height)

    override fun dispose() {
        ptr.destroy()
        //directByteBuffer.clear()
        printdbg(this, "BlockLayer successfully freed")
    }

    internal fun getPtr() = ptr.ptr

    companion object {
        @Transient val BYTES_PER_BLOCK = 2L
    }
}

fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
