package net.torvald.terrarum.gameworld

import net.torvald.terrarum.gameworld.GameWorld.Companion.TERRAIN
import net.torvald.terrarum.gameworld.GameWorld.Companion.WALL
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.serialise.toUint

/**
 * Memory layout:
 * ```
 *  a7 a6 a5 a4 a3 a2 a1 a0 | aF aE aD aC aB aA a9 a8 ||
 * ```
 * where a_n is a tile number
 *
 * Original version Created by minjaesong on 2016-01-17.
 * Unsafe version Created by minjaesong on 2019-06-08.
 * Chunkpool version Created by minjaesong on 2024-10-22.
 *
 * Note to self: refrain from using shorts--just do away with two bytes: different system have different endianness
 */
class BlockLayerGenericI16: BlockLayerWithChunkPool {

    override val width: Int
    override val height: Int
    override val chunkPool: ChunkPool

    private var _hashcode = 0

    constructor(
        width: Int,
        height: Int,
        disk: ClusteredFormatDOM,
        layerNum: Int,
        world: TheGameWorld
    ) {
        this.width = width
        this.height = height

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, -1, when (layerNum) {
            TERRAIN -> ChunkPool.getRenameFunTerrain(world)
            WALL -> ChunkPool.getRenameFunTerrain(world)
            else -> throw IllegalArgumentException("Unknown layer number for I16: $layerNum")
        })

        _hashcode = disk.uuid.hashCode()
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

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, -1, when (layerNum) {
            TERRAIN -> ChunkPool.getRenameFunTerrain(world)
            WALL -> ChunkPool.getRenameFunTerrain(world)
            else -> throw IllegalArgumentException("Unknown layer number for I16: $layerNum")
        })

        _hashcode = disk.diskFile.hashCode()
    }

    override fun hashCode() = _hashcode

    override val bytesPerBlock = BYTES_PER_BLOCK

    override fun unsafeGetTile(x: Int, y: Int): Int {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        return chunkPool.getTileI16(chunk, ox, oy)
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val bytes = unsafeGetTile(x, y)
        return byteArrayOf(
            ((bytes ushr 8) and 255).toByte(),
            (bytes and 255).toByte()
        )
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int) {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        chunkPool.setTileRaw(chunk, ox, oy, tile)
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int, fill: Float) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, tile: Int, placement: Int) {
        throw UnsupportedOperationException()
    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val tile = (0..1).fold(0) { acc, i -> acc or (bytes[i].toUint()).shl(8*i) }
        unsafeSetTile(x, y, tile)
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

    override fun toString(): String = "BlockLayerI16 (${width}x$height)"

    companion object {
        @Transient val BYTES_PER_BLOCK = 2L
    }
}
