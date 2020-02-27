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

    val TOTAL_SIZE_IN_BYTES = 16L * width * height

    val array = UnsafeHelper.allocate(TOTAL_SIZE_IN_BYTES)

    private inline fun toAddr(x: Int, y: Int) = 16L * (y * width + x)

    fun zerofill() = array.fillWith(0)

    init {
        zerofill()
    }

    fun getR(x: Int, y: Int) = array.getFloat(toAddr(x, y))
    fun getG(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 4)
    fun getB(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 8)
    fun getA(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 12)

    fun setR(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y), value) }
    fun setG(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 4, value) }
    fun setB(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 8, value) }
    fun setA(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 12, value) }

    fun addA(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 12, getA(x, y) + value) }

    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    inline fun channelSet(x: Int, y: Int, channel: Int, value: Float) {
        array.setFloat(toAddr(x, y) + 4L * channel, value)
    }

    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    inline fun channelGet(x: Int, y: Int, channel: Int) = array.getFloat(toAddr(x, y) + 4L * channel)

    fun max(x: Int, y: Int, other: Cvec) {
        setR(x, y, maxOf(getR(x, y), other.r))
        setG(x, y, maxOf(getG(x, y), other.g))
        setB(x, y, maxOf(getB(x, y), other.b))
        setA(x, y, maxOf(getA(x, y), other.a))
    }

    fun destroy() = this.array.destroy()

}