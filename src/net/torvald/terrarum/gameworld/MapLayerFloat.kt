package net.torvald.terrarum.gameworld

import net.torvald.dataclass.Float16
import net.torvald.dataclass.Float16Bits

/**
 * MapLayer that contains raw Float16 values
 *
 * Created by SKYHi14 on 2017-04-21.
 */
class MapLayerFloat(val width: Int, val height: Int) : Iterable<Float16Bits> {

    constructor(width: Int, height: Int, init: Float) : this(width, height) {
        data = Array(height) { Array(width, { Float16.fromFloat(init) }) }
    }

    internal @Volatile var data: Array<Array<Float16Bits>> // in parallel programming: do not trust your register; always read freshly from RAM!

    init {
        data = Array(height) { Array(width, { 0.toShort() }) }
    }

    /**
     * Returns an iterator over elements of type `T`.

     * @return an Iterator.
     */
    override fun iterator(): Iterator<Float16Bits> {
        return object : Iterator<Float16Bits> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): Float16Bits {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return data[y][x]
            }
        }
    }

    internal fun getValue(x: Int, y: Int): Float? {
        return if (x !in 0..width - 1 || y !in 0..height - 1)
            null
        else
            Float16.toFloat(data[y][x])
    }

    internal fun setValue(x: Int, y: Int, value: Float) {
        data[y][x] = Float16.fromFloat(value)
    }

    fun isInBound(x: Int, y: Int) = (x >= 0 && y >= 0 && x < width && y < height)

    companion object {
        @Transient const val SIZEOF: Byte = 2 // 1 for 8-bit, 2 for 16-bit, ...
    }
}