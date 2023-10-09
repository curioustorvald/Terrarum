package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr

/**
 * Memory layout:
 * ```
 *  a7 a6 a5 a4 a3 a2 a1 a0 | aF aE aD aC aB aA a9 a8 ||
 * ```
 * where a_n is a tile number
 *
 * Original version Created by minjaesong on 2016-01-17.
 * Unsafe version Created by minjaesong on 2019-06-08.
 *
 * Note to self: refrain from using shorts--just do away with two bytes: different system have different endianness
 */
open class BlockLayerI16(val width: Int, val height: Int) : BlockLayer {
    override val bytesPerBlock = BYTES_PER_BLOCK

    // for some reason, all the efforts of saving the memory space were futile.

    // using unsafe pointer gets you 100 fps, whereas using directbytebuffer gets you 90
    internal val ptr: UnsafePtr = UnsafeHelper.allocate(width * height * BYTES_PER_BLOCK)

    val ptrDestroyed: Boolean
        get() = ptr.destroyed

    init {
        ptr.fillWith(0)
    }

    /**
     * @param data Byte array representation of the layer
     */
    constructor(width: Int, height: Int, data: ByteArray) : this(width, height) {
        TODO()
        data.forEachIndexed { index, byte -> UnsafeHelper.unsafe.putByte(ptr.ptr + index, byte) }
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
                return iteratorCount < width * height * BYTES_PER_BLOCK
            }
            override fun next(): Byte {
                iteratorCount += 1
                return ptr[iteratorCount - 1]
            }
        }
    }

    internal fun unsafeGetTile(x: Int, y: Int): Int {
        val offset = BYTES_PER_BLOCK * (y * width + x)
        val lsb = ptr[offset]
        val msb = ptr[offset + 1]

        return lsb.toUint() + msb.toUint().shl(8)
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val offset = BYTES_PER_BLOCK * (y * width + x)
        return byteArrayOf(ptr[offset + 1], ptr[offset + 0])
    }

    internal fun unsafeSetTile(x: Int, y: Int, tile: Int) {
        val offset = BYTES_PER_BLOCK * (y * width + x)

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()


//        try {
            ptr[offset] = lsb
            ptr[offset + 1] = msb
//        }
//        catch (e: IndexOutOfBoundsException) {
//            printdbgerr(this, "IndexOutOfBoundsException: x = $x, y = $y; offset = $offset")
//            throw e
//        }
    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val offset = BYTES_PER_BLOCK * (y * width + x)
        ptr[offset] = bytes[1]
        ptr[offset + 1] = bytes[0]
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
        printdbg(this, "BlockLayerI16 with ptr ($ptr) successfully freed")
    }

    override fun toString(): String = ptr.toString("BlockLayerI16")

    companion object {
        @Transient val BYTES_PER_BLOCK = 2L
    }
}
