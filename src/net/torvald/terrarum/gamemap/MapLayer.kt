package net.torvald.terrarum.gamemap

/**
 * Created by minjaesong on 16-01-17.
 */
class MapLayer(var width: Int, var height: Int) : Iterable<Byte> {

    internal var data: Array<ByteArray>

    init {
        data = Array(height) { ByteArray(width) }
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

                return data[y][x]
            }
        }
    }

    internal fun getTile(x: Int, y: Int): Int? {
        return if (x !in 0..width - 1 || y !in 0..height - 1)
            null
        else
            data[y][x].toUint()
    }

    internal fun setTile(x: Int, y: Int, tile: Byte) {
        data[y][x] = tile
    }

    fun isInBound(x: Int, y: Int) = (x >= 0 && y >= 0 && x < width && y < height)

    companion object {
        @Transient const val RANGE = 256
        @Transient const val SIZEOF: Byte = 1 // 1 for 8-bit, 2 for 16-bit, ...
    }
}

fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
