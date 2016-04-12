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
            uint8ToInt32(data[y][x])
    }

    internal fun setTile(x: Int, y: Int, tile: Byte) {
        data[y][x] = tile
    }

    private fun uint8ToInt32(x: Byte): Int = java.lang.Byte.toUnsignedInt(x)

    companion object {
        @Transient val RANGE = 256
    }
}

