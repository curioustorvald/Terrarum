package net.torvald.gdx.graphics

import net.torvald.UnsafeHelper

/**
 * Basically just a FloatArray. You may need to re-write the entire code to actually store the Vectors,
 * maybe in the form of array of objects.
 */

/**
 * Created by minjaesong on 2019-06-21.
 */
internal class UnsafeCvecArray(val width: Int, val height: Int) {

    val sizeof = 16L

    val TOTAL_SIZE_IN_BYTES = sizeof * width * height

    val array = UnsafeHelper.allocate(TOTAL_SIZE_IN_BYTES)

    private inline fun toAddr(x: Int, y: Int) = sizeof * (y * width + x)

    fun zerofill() = array.fillWith(0)

    init {
        zerofill()
    }

    fun getVec(x: Int, y: Int): Cvec {
        val addr = toAddr(x, y)
        return Cvec(
                array.getFloat(addr),
                array.getFloat(addr + 4),
                array.getFloat(addr + 8),
                array.getFloat(addr + 12)
        )
    }
    fun setVec(x: Int, y: Int, value: Cvec) {
        val addr = toAddr(x, y)
        array.setFloat(addr, value.vec.lane(0))
        array.setFloat(addr + 4, value.vec.lane(1))
        array.setFloat(addr + 8, value.vec.lane(2))
        array.setFloat(addr + 12, value.vec.lane(3))
    }

    fun destroy() = this.array.destroy()

}