package net.torvald.terrarum.gameworld

import net.torvald.terrarum.virtualcomputer.tvd.ByteArray64

/**
 * Created by minjaesong on 16-01-17.
 */
class MapLayer(val width: Int, val height: Int) : Iterable<Byte> {

    internal @Volatile var data: ByteArray // in parallel programming: do not trust your register; always read freshly from RAM!

    init {
        data = ByteArray(width * height)
    }

    /**
     * Returns an iterator over elements of type `T`.

     * @return an Iterator.
     */
    override fun iterator(): Iterator<Byte> {
        return object : Iterator<Byte> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): Byte {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return data[y * width + x]
            }
        }
    }

    internal fun getTile(x: Int, y: Int): Int? {
        return if (x !in 0..width - 1 || y !in 0..height - 1)
            null
        else
            data[y * width + x].toUint()
    }

    internal fun setTile(x: Int, y: Int, tile: Byte) {
        data[y * width + x] = tile
    }

    fun isInBound(x: Int, y: Int) = (x >= 0 && y >= 0 && x < width && y < height)

    companion object {
        @Transient const val RANGE = 256
        @Transient const val SIZEOF: Byte = 1 // 1 for 8-bit, 2 for 16-bit, ...
    }
}

fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
