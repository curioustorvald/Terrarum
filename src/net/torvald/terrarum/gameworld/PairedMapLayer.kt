package net.torvald.terrarum.gameworld

/**
 * Created by minjaesong on 2016-02-15.
 */
open class PairedMapLayer : Iterable<Byte> {

    val width: Int; val height: Int


    /**
     * 0b_xxxx_yyyy, x for lower index, y for higher index

     * e.g.

     * 0110 1101 is interpreted as
     * 6 for tile 0, 13 for tile 1.
     */
    internal @Volatile var data: ByteArray

    constructor(width: Int, height: Int) {
        this.width = width / 2
        this.height = height
        data = ByteArray(width * height / 2)
    }

    constructor(width: Int, height: Int, data: ByteArray) {
        this.data = data
        this.width = width / 2
        this.height = height
    }


    /**
     * Returns an iterator over elements of type `T`.
     * Note: this iterator will return combined damage, that is 0bxxxx_yyyy as whole.

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

    internal fun getData(x: Int, y: Int): Int? {
        return if (x !in 0..width * 2 - 1 || y !in 0..height - 1)
            null
        else {
            if (x and 0x1 == 0)
            // higher four bits for i = 0, 2, 4, ...
                (java.lang.Byte.toUnsignedInt(data[y * width + x / 2]) and 0xF0) ushr 4
            else
            // lower four bits for i = 1, 3, 5, ...
                java.lang.Byte.toUnsignedInt(data[y * width + x / 2]) and 0x0F
        }
    }

    internal fun setData(x: Int, y: Int, data: Int) {
        if (data < 0 || data >= 16) throw IllegalArgumentException("[PairedMapLayer] $data: invalid data value.")
        if (x and 0x1 == 0)
        // higher four bits for i = 0, 2, 4, ...
            this.data[y * width + x / 2] =
                    (java.lang.Byte.toUnsignedInt(this.data[y * width + x / 2]) and 0x0F
                            or (data and 0xF shl 4)).toByte()
        else
        // lower four bits for i = 1, 3, 5, ...
            this.data[y * width + x / 2] = (java.lang.Byte.toUnsignedInt(this.data[y * width + x / 2]) and 0xF0
                    or (data and 0xF)).toByte()
    }

    companion object {
        @Transient const val RANGE = 16
        @Transient const val SIZEOF: Byte = 1 // 1 for 8-bit, 2 for 16-bit, ...
    }
}
