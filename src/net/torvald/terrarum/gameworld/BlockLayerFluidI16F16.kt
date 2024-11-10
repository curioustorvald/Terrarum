package net.torvald.terrarum.gameworld

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.serialise.toUint
import net.torvald.util.Float16

const val FLUID_MIN_MASS = 1f / 1024f //Ignore cells that are almost dry (smaller than epsilon of float16)

/**
 *  * Memory layout:
 *  * ```
 *  *  a7 a6 a5 a4 a3 a2 a1 a0 | aF aE aD aC aB aA a9 a8 | f7 f6 f5 f4 f3 f2 f1 f0 | fF fE fD fC fB fA f9 f8 ||
 *  * ```
 *  * where a_n is a fluid number, f_n is a fluid fill
 *
 * Unsafe version Created by minjaesong on 2023-10-10.
 * Chunkpool version Created by minjaesong on 2024-10-22.
 */
class BlockLayerFluidI16F16 : BlockLayerWithChunkPool {

    override val width: Int
    override val height: Int
    override val chunkPool: ChunkPool

    constructor(
        width: Int,
        height: Int,
        disk: ClusteredFormatDOM,
        layerNum: Int,
        world: TheGameWorld
    ) {
        this.width = width
        this.height = height

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, -1, ChunkPool.getRenameFunFluids(world))
    }

    constructor(
        width: Int,
        height: Int,
        disk: DiskSkimmer,
        layerNum: Int,
        world: TheGameWorld
    ) {
        this.width = width
        this.height = height

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, -1, ChunkPool.getRenameFunFluids(world))
    }


    override val bytesPerBlock = BYTES_PER_BLOCK

    override fun unsafeGetTile(x: Int, y: Int): Int {
        return unsafeGetTileI16F16(x, y).first
    }

    override fun unsafeGetTileI16F16(x: Int, y: Int): Pair<Int, Float> {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        return chunkPool.getTileI16F16(chunk, ox, oy)
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val (tile, fill0) = unsafeGetTileI16F16(x, y)
        val fill = Float16.fromFloat(fill0).toUint()
        return byteArrayOf(
            ((tile ushr 8) and 255).toByte(),
            (tile and 255).toByte(),
            ((fill ushr 8) and 255).toByte(),
            (fill and 255).toByte(),        )
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int, placement: Int) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, tile0: Int, fill: Float) {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        val fill = Float16.fromFloat(fill).toUint()
        chunkPool.setTileRaw(chunk, ox, oy, tile0.and(65535) or fill.shl(16))
    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val tile = bytes[1].toUint().shl(8) or bytes[0].toUint()
        val fill = Float16.toFloat((bytes[3].toUint().shl(8) or bytes[2].toUint()).toShort())
        unsafeSetTile(x, y, tile, fill)
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

    override var disposed: Boolean = false

    override fun dispose() {
        chunkPool.dispose()
        disposed = true
    }

    override fun toString(): String = "BlockLayerI16F16 (${width}x$height)"

    companion object {
        @Transient val BYTES_PER_BLOCK = 4L

        private fun Short.toUint() = this.toInt().and(65535)
    }
}
