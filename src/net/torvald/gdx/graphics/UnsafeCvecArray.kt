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

    private val TOTAL_SIZE_IN_BYTES = 16L * (width + 1) * (height + 1)
    private val array = UnsafeHelper.allocate(TOTAL_SIZE_IN_BYTES)

    private inline fun toAddr(x: Int, y: Int) = 16L * (y * width + x)

    init {
        zerofill()
    }

    // getters
    fun getR(x: Int, y: Int) = array.getFloat(toAddr(x, y))
    fun getG(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 4)
    fun getB(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 8)
    fun getA(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 12)
    inline fun getVec(x: Int, y: Int) = Cvec(
            array.getFloat(toAddr(x, y)),
            array.getFloat(toAddr(x, y) + 4),
            array.getFloat(toAddr(x, y) + 8),
            array.getFloat(toAddr(x, y) + 12)
    )
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    fun channelGet(x: Int, y: Int, channel: Int) = array.getFloat(toAddr(x, y) + 4L * channel)

    // setters
    fun zerofill() = array.fillWith(0)
    fun setR(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y), value) }
    fun setG(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 4, value) }
    fun setB(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 8, value) }
    fun setA(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 12, value) }
    inline fun setVec(x: Int, y: Int, value: Cvec) {
        array.setFloat(toAddr(x, y), value.r)
        array.setFloat(toAddr(x, y) + 4, value.g)
        array.setFloat(toAddr(x, y) + 8, value.b)
        array.setFloat(toAddr(x, y) + 12, value.a)
    }
    inline fun setScalar(x: Int, y: Int, value: Float) {
        array.setFloat(toAddr(x, y), value)
        array.setFloat(toAddr(x, y) + 4, value)
        array.setFloat(toAddr(x, y) + 8, value)
        array.setFloat(toAddr(x, y) + 12, value)
    }
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    fun channelSet(x: Int, y: Int, channel: Int, value: Float) {
        array.setFloat(toAddr(x, y) + 4L * channel, value)
    }

    // operators
    inline fun max(x: Int, y: Int, other: Cvec) {
        setR(x, y, maxOf(getR(x, y), other.r))
        setG(x, y, maxOf(getG(x, y), other.g))
        setB(x, y, maxOf(getB(x, y), other.b))
        setA(x, y, maxOf(getA(x, y), other.a))
    }
    inline fun mul(x: Int, y: Int, scalar: Float) {
        setR(x, y, getR(x, y) * scalar)
        setG(x, y, getG(x, y) * scalar)
        setB(x, y, getB(x, y) * scalar)
        setA(x, y, getA(x, y) * scalar)
    }

    fun destroy() = this.array.destroy()

}


/**
 * Safe (and slower) version of UnsafeCvecArray utilised to tackle down the SEGFAULT
 */
internal class TestCvecArr(val width: Int, val height: Int) {

    val TOTAL_SIZE_IN_BYTES = 4 * width * height

    val array = FloatArray(TOTAL_SIZE_IN_BYTES)

    private inline fun toAddr(x: Int, y: Int) = 4 * (y * width + x)

    init {
        zerofill()
    }

    // getters
    fun getR(x: Int, y: Int) = array.get(toAddr(x, y))
    fun getG(x: Int, y: Int) = array.get(toAddr(x, y) + 1)
    fun getB(x: Int, y: Int) = array.get(toAddr(x, y) + 2)
    fun getA(x: Int, y: Int) = array.get(toAddr(x, y) + 3)
    inline fun getVec(x: Int, y: Int) = Cvec(
            array.get(toAddr(x, y)),
            array.get(toAddr(x, y) + 1),
            array.get(toAddr(x, y) + 2),
            array.get(toAddr(x, y) + 3)
    )
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    fun channelGet(x: Int, y: Int, channel: Int) = array.get(toAddr(x, y) + 1 * channel)

    // setters
    fun zerofill() = array.fill(0f)
    fun setR(x: Int, y: Int, value: Float) { array.set(toAddr(x, y), value) }
    fun setG(x: Int, y: Int, value: Float) { array.set(toAddr(x, y) + 1, value) }
    fun setB(x: Int, y: Int, value: Float) { array.set(toAddr(x, y) + 2, value) }
    fun setA(x: Int, y: Int, value: Float) { array.set(toAddr(x, y) + 3, value) }
    inline fun setVec(x: Int, y: Int, value: Cvec) {
        array.set(toAddr(x, y), value.r)
        array.set(toAddr(x, y) + 1, value.g)
        array.set(toAddr(x, y) + 2, value.b)
        array.set(toAddr(x, y) + 3, value.a)
    }
    inline fun setScalar(x: Int, y: Int, value: Float) {
        array.set(toAddr(x, y), value)
        array.set(toAddr(x, y) + 1, value)
        array.set(toAddr(x, y) + 2, value)
        array.set(toAddr(x, y) + 3, value)
    }
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    fun channelSet(x: Int, y: Int, channel: Int, value: Float) {
        array.set(toAddr(x, y) + 1 * channel, value)
    }

    // operators
    inline fun max(x: Int, y: Int, other: Cvec) {
        setR(x, y, maxOf(getR(x, y), other.r))
        setG(x, y, maxOf(getG(x, y), other.g))
        setB(x, y, maxOf(getB(x, y), other.b))
        setA(x, y, maxOf(getA(x, y), other.a))
    }
    inline fun mul(x: Int, y: Int, scalar: Float) {
        setR(x, y, getR(x, y) * scalar)
        setG(x, y, getG(x, y) * scalar)
        setB(x, y, getB(x, y) * scalar)
        setA(x, y, getA(x, y) * scalar)
    }

    fun destroy() = {}

}