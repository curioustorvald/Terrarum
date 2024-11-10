package net.torvald.terrarum.gameworld

import net.torvald.terrarum.App
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr

/**
 * Memory layout:
 * ```
 *  a7 a6 a5 a4 a3 a2 a1 a0 | aF aE aD aC aB aA a9 a8 | p7 p6 p5 p4 p3 p2 p1 p0 ||
 * ```
 * where a_n is a tile number, p_n is a placement index
 * Created by minjaesong on 2023-10-10.
 */
class BlockLayerInMemoryI16I8 (override val width: Int, override val height: Int) : BlockLayer() {
    override val bytesPerBlock = BYTES_PER_BLOCK

    // for some reason, all the efforts of saving the memory space were futile.

    // using unsafe pointer gets you 100 fps, whereas using directbytebuffer gets you 90
    internal val ptr: UnsafePtr = UnsafeHelper.allocate(width * height * bytesPerBlock)

    val ptrDestroyed: Boolean
        get() = ptr.destroyed

    init {
        ptr.fillWith(0) // there is no NOT-GENERATED for ores, keep it as 0
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
                return iteratorCount < width * height * bytesPerBlock
            }
            override fun next(): Byte {
                iteratorCount += 1
                return ptr[iteratorCount - 1]
            }
        }
    }

    override fun unsafeGetTile(x: Int, y: Int): Int {
        val offset = getOffset(x, y)
        val lsb = ptr[offset]
        val msb = ptr[offset + 1]
        val placement = ptr[offset + 2]

        return lsb.toUint() + msb.toUint().shl(8)
    }

    internal fun unsafeGetTile1(x: Int, y: Int): Pair<Int, Int> {
        val offset = getOffset(x, y)
        val lsb = ptr[offset]
        val msb = ptr[offset + 1]
        val placement = ptr[offset + 2]

        return lsb.toUint() or msb.toUint().shl(8) to placement.toUint()
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val offset = getOffset(x, y)
        return byteArrayOf(ptr[offset + 1], ptr[offset + 0], ptr[offset + 2])
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int, fill: Float) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int, placement: Int) {
        val offset = getOffset(x, y)

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()


//        try {
        ptr[offset] = lsb
        ptr[offset + 1] = msb
        ptr[offset + 2] = placement.toByte()
//        }
//        catch (e: IndexOutOfBoundsException) {
//            printdbgerr(this, "IndexOutOfBoundsException: x = $x, y = $y; offset = $offset")
//            throw e
//        }
    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val offset = getOffset(x, y)
        ptr[offset] = bytes[1]
        ptr[offset + 1] = bytes[0]
        ptr[offset + 2] = bytes[2]
    }

    override fun unsafeSetTileKeepOrePlacement(x: Int, y: Int, tile: Int) {
        val offset = getOffset(x, y)

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()

        ptr[offset] = lsb
        ptr[offset + 1] = msb
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

    override val disposed: Boolean
        get() = ptr.destroyed

    override fun dispose() {
        ptr.destroy()
        App.printdbg(this, "BlockLayerI16I8 with ptr ($ptr) successfully freed")
    }

    override fun toString(): String = ptr.toString("BlockLayerI16I8")

    companion object {
        @Transient val BYTES_PER_BLOCK = 3L
    }
}
