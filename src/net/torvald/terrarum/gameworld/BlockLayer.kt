package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2023-10-10.
 */
abstract class BlockLayer : Disposable {
    abstract val chunkPool: ChunkPool
    abstract val width: Int
    abstract val height: Int
    abstract val bytesPerBlock: Long
    abstract fun unsafeToBytes(x: Int, y: Int): ByteArray
    abstract fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray)
    abstract fun unsafeGetTile(x: Int, y: Int): Int

}

inline fun BlockLayer.getOffset(x: Int, y: Int): Long {
    return this.bytesPerBlock * (y * this.width + x)
}