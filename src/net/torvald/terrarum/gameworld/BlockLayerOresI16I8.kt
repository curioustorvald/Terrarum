package net.torvald.terrarum.gameworld

import net.torvald.terrarum.App
import net.torvald.terrarum.gameworld.BlockLayerFluidI16F16.Companion
import net.torvald.terrarum.gameworld.GameWorld.Companion.TERRAIN
import net.torvald.terrarum.gameworld.GameWorld.Companion.WALL
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.savegame.DiskSkimmer
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
class BlockLayerOresI16I8 : BlockLayer {

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

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, 0, ChunkPool.getRenameFunOres(world))
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

        chunkPool = ChunkPool(disk, layerNum, BYTES_PER_BLOCK, world, 0, ChunkPool.getRenameFunOres(world))
    }


    override val bytesPerBlock = BYTES_PER_BLOCK

    override fun unsafeGetTile(x: Int, y: Int): Int {
        return unsafeGetTile1(x, y).first
    }

    internal fun unsafeGetTile1(x: Int, y: Int): Pair<Int, Int> {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        return chunkPool.getTileI16I8(chunk, ox, oy)
    }

    override fun unsafeToBytes(x: Int, y: Int): ByteArray {
        val (tile, fill) = unsafeGetTile1(x, y)
        return byteArrayOf(
            ((tile ushr 8) and 255).toByte(),
            (tile and 255).toByte(),
            (fill and 255).toByte()
        )
    }

    internal fun unsafeSetTile(x: Int, y: Int, tile: Int, placement: Int) {
        val (chunk, ox, oy) = chunkPool.worldXYChunkNumAndOffset(x, y)
        chunkPool.setTileRaw(chunk, ox, oy, tile or placement.shl(16))
    }

    override fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {
        val tile = bytes[1].toUint().shl(8) or bytes[0].toUint()
        val placement = bytes[2].toUint()
        unsafeSetTile(x, y, tile, placement)
    }

    internal fun unsafeSetTileKeepPlacement(x: Int, y: Int, tile: Int) {
        val oldPlacement = unsafeGetTile1(x, y).second
        unsafeSetTile(x, y, tile, oldPlacement)
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
        chunkPool.dispose()
    }

    override fun toString(): String = "BlockLayerI16I8 (${width}x$height)"

    companion object {
        @Transient val BYTES_PER_BLOCK = 3L
    }
}
