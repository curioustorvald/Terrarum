package net.torvald.terrarum.gameworld

import net.torvald.terrarum.App
import net.torvald.terrarum.gameworld.BlockLayerGenericI16.Companion
import net.torvald.terrarum.gameworld.GameWorld.Companion.TERRAIN
import net.torvald.terrarum.gameworld.GameWorld.Companion.WALL
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import net.torvald.util.Float16

const val FLUID_MIN_MASS = 1f / 1024f //Ignore cells that are almost dry (smaller than epsilon of float16)

/**
 *  * Memory layout:
 *  * ```
 *  *  a7 a6 a5 a4 a3 a2 a1 a0 | aF aE aD aC aB aA a9 a8 | f7 f6 f5 f4 f3 f2 f1 f0 | fF fE fD fC fB fA f9 f8 ||
 *  * ```
 *  * where a_n is a fluid number, f_n is a fluid fill
 *
 * Created by minjaesong on 2023-10-10.
 */
class BlockLayerFluidI16F16 : BlockLayer {

    override val width: Int
    override val height: Int
    override val chunkPool: ChunkPool

    constructor(
        width: Int,
        height: Int,
        disk: ClusteredFormatDOM,
        layerNum: Int,
        world: GameWorld
    ) {
        this.width = width
        this.height = height

        chunkPool = ChunkPool(disk, layerNum, BlockLayerGenericI16.BYTES_PER_BLOCK, world, -1, ChunkPool.getRenameFunFluids(world))
    }

    constructor(
        width: Int,
        height: Int,
        disk: DiskSkimmer,
        layerNum: Int,
        world: GameWorld
    ) {
        this.width = width
        this.height = height

        chunkPool = ChunkPool(disk, layerNum, BlockLayerGenericI16.BYTES_PER_BLOCK, world, -1, ChunkPool.getRenameFunFluids(world))
    }


    override val bytesPerBlock = BYTES_PER_BLOCK

    internal val ptr: UnsafePtr = UnsafeHelper.allocate(width * height * bytesPerBlock)

    val ptrDestroyed: Boolean
        get() = ptr.destroyed

    init {
        ptr.fillWith(-1)
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

        return lsb.toUint() or msb.toUint().shl(8)
    }

    internal fun unsafeGetTile1(x: Int, y: Int): Pair<Int, Float> {
        val offset = getOffset(x, y)
        val lsb = ptr[offset]
        val msb = ptr[offset + 1]
        val hbits = (ptr[offset + 2].toUint() or ptr[offset + 3].toUint().shl(8)).toShort()
        val fill = Float16.toFloat(hbits)

        return lsb.toUint() or msb.toUint().shl(8) to fill
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val offset = getOffset(x, y)
        return byteArrayOf(ptr[offset + 1], ptr[offset + 0], ptr[offset + 3], ptr[offset + 2])
    }

    internal fun unsafeSetTile(x: Int, y: Int, tile0: Int, fill: Float) {
        val offset = getOffset(x, y)
        val hbits = Float16.fromFloat(fill).toInt().and(0xFFFF)

        val tile = if (fill < FLUID_MIN_MASS) 0 else tile0

        val lsb = tile.and(0xff).toByte()
        val msb = tile.ushr(8).and(0xff).toByte()

        val hlsb = hbits.and(0xff).toByte()
        val hmsb = hbits.ushr(8).and(0xff).toByte()

        ptr[offset] = lsb
        ptr[offset + 1] = msb
        ptr[offset + 2] = hlsb
        ptr[offset + 3] = hmsb

    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val offset = getOffset(x, y)
        ptr[offset] = bytes[1]
        ptr[offset + 1] = bytes[0]
        ptr[offset + 2] = bytes[3]
        ptr[offset + 3] = bytes[2]
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
        App.printdbg(this, "BlockLayerI16F16 with ptr ($ptr) successfully freed")
    }

    override fun toString(): String = ptr.toString("BlockLayerI16F16")

    companion object {
        @Transient val BYTES_PER_BLOCK = 4L
    }
}
