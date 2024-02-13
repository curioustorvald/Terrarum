package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2023-10-10.
 */
interface BlockLayer : Disposable {

    val width: Int
    val height: Int
    val bytesPerBlock: Long
    fun unsafeToBytes(x: Int, y: Int): ByteArray
    fun unsafeSetTile(x: Int, y: Int, bytes: ByteArray)
    fun unsafeGetTile(x: Int, y: Int): Int

}

inline fun BlockLayer.getOffset(x: Int, y: Int): Long {
    return this.bytesPerBlock * (y * this.width + x)
}