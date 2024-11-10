package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2023-10-10.
 */
open class BlockLayer() : Disposable {
//    abstract val chunkPool: ChunkPool
    open val width: Int = 0
    open val height: Int = 0
    open val bytesPerBlock: Long = 0
    open fun unsafeToBytes(x: Int, y: Int): ByteArray = byteArrayOf(0,0,0,0)
    open fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray) {  }
    // for I16; other layer must throw UnsupportedOperationException
    open fun unsafeSetTile(x: Int, y: Int, tile: Int) {  }
    // for I16F16; other layer must throw UnsupportedOperationException
    open fun unsafeSetTile(x: Int, y: Int, tile: Int, fill: Float) {  }
    // for I16I8; other layer must throw UnsupportedOperationException
    open fun unsafeSetTile(x: Int, y: Int, tile: Int, placement: Int) {  }

    open fun unsafeGetTile(x: Int, y: Int): Int = 0

    open fun getOffset(x: Int, y: Int): Long {
        return this.bytesPerBlock * (y * this.width + x)
    }

    open val disposed: Boolean = false
    override fun dispose() {
    }

    open fun unsafeGetTileI16F16(x: Int, y: Int): Pair<Int, Float> = 0 to 0f
    open fun unsafeGetTileI16I8(x: Int, y: Int): Pair<Int, Int> = 0 to 0

    open fun unsafeSetTileKeepOrePlacement(x: Int, y: Int, tile: Int) {  }

}

abstract class BlockLayerWithChunkPool : BlockLayer() {
    abstract val chunkPool: ChunkPool

    /**
     * Unsupported for BlockLayerWithChunkPool
     */
    override fun getOffset(x: Int, y: Int): Long {
        throw UnsupportedOperationException()
    }
}

/*inline fun BlockLayer.getOffset(x: Int, y: Int): Long {
    return this.bytesPerBlock * (y * this.width + x)
}*/