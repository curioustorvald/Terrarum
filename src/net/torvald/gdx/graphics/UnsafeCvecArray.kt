package net.torvald.gdx.graphics

import net.torvald.unsafe.UnsafeHelper

/**
 * Basically just a FloatArray. You may need to re-write the entire code to actually store the Vectors,
 * maybe in the form of array of objects.
 */

/**
 * Created by minjaesong on 2019-06-21.
 */
internal class UnsafeCvecArray(val width: Int, val height: Int) {

    val TOTAL_SIZE_IN_BYTES = 16L * (width + 1) * (height + 1)
    private val array = UnsafeHelper.allocate(TOTAL_SIZE_IN_BYTES)
    val ptr = array.ptr

    private inline fun toAddr(x: Int, y: Int) = 4L * (y * width + x)

    fun isDestroyed() = array.destroyed

    init {
        zerofill()
    }

    // getters
//    fun getR(x: Int, y: Int) = array.getFloat(toAddr(x, y))
//    fun getG(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 1)
//    fun getB(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 2)
//    fun getA(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 3)
//    operator fun get(i: Long) = array.getFloat(i)
    /**
     * Returns a copy of the vector. Use [setVec] to modify the value in the CvecArray
     */
    fun getVec(x: Int, y: Int): Cvec {
        val a = toAddr(x, y)
        return Cvec(
                array.getFloat(a + 0),
                array.getFloat(a + 1),
                array.getFloat(a + 2),
                array.getFloat(a + 3)
        )
    }

    /**
     * `getAndSet(cvec, x, y)` is equivalent to
     * `cvec.set(this.getVec(x, y))`
     */
    fun getAndSet(target: Cvec, x: Int, y: Int) {
        val a = toAddr(x, y)
        target.r = array.getFloat(a + 0)
        target.g = array.getFloat(a + 1)
        target.b = array.getFloat(a + 2)
        target.a = array.getFloat(a + 3)
        checkNaN(target)
    }
    /**
     * `getAndSet(cvec, x, y, func)` is equivalent to
     * `target.setVec(x, y, func(this.getVec(x, y)))`
     *
     * The target must have the same dimension as this CvecArray.
     */
    fun getAndSetMap(target: UnsafeCvecArray, x: Int, y: Int, transform: (Float) -> Float) {
        val a = toAddr(x, y)
        target.array.setFloat(a + 0, transform(this.array.getFloat(a + 0)))
        target.array.setFloat(a + 1, transform(this.array.getFloat(a + 1)))
        target.array.setFloat(a + 2, transform(this.array.getFloat(a + 2)))
        target.array.setFloat(a + 3, transform(this.array.getFloat(a + 3)))
    }
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
//    fun channelGet(x: Int, y: Int, channel: Int) = array.getFloat(toAddr(x, y) + channel)

    // setters
    fun zerofill() = array.fillWith(0)
//    fun setR(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y), value) }
//    fun setG(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 1, value) }
//    fun setB(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 2, value) }
//    fun setA(x: Int, y: Int, value: Float) { array.setFloat(toAddr(x, y) + 3, value) }
//    operator fun set(i: Long, value: Float) = array.setFloat(i, value)
    fun setVec(x: Int, y: Int, value: Cvec) {
        checkNaN(value)
        val a = toAddr(x, y)
        array.setFloat(a + 0, value.r)
        array.setFloat(a + 1, value.g)
        array.setFloat(a + 2, value.b)
        array.setFloat(a + 3, value.a)
    }
    fun setScalar(x: Int, y: Int, value: Float) {
        checkNaN(value)

        val a = toAddr(x, y)

        array.setFloat(a + 0, value)
        array.setFloat(a + 1, value)
        array.setFloat(a + 2, value)
        array.setFloat(a + 3, value)
    }
    /**
     * @param channel 0 for R, 1 for G, 2 for B, 3 for A
     */
    fun channelSet(x: Int, y: Int, channel: Int, value: Float) {
        array.setFloat(toAddr(x, y) + channel, value)
    }

    // operators
    fun max(x: Int, y: Int, other: Cvec) {
        checkNaN(other)
        val a = toAddr(x, y)
        array.setFloat(a + 0, kotlin.math.max(array.getFloat(a + 0), other.r))
        array.setFloat(a + 1, kotlin.math.max(array.getFloat(a + 1), other.g))
        array.setFloat(a + 2, kotlin.math.max(array.getFloat(a + 2), other.b))
        array.setFloat(a + 3, kotlin.math.max(array.getFloat(a + 3), other.a))
    }
    fun mul(x: Int, y: Int, scalar: Float) {
        checkNaN(scalar)
        val a = toAddr(x, y)
        array.setFloat(a + 0, (array.getFloat(a + 0) * scalar))
        array.setFloat(a + 1, (array.getFloat(a + 1) * scalar))
        array.setFloat(a + 2, (array.getFloat(a + 2) * scalar))
        array.setFloat(a + 3, (array.getFloat(a + 3) * scalar))
    }

    fun mulAndAssign(x: Int, y: Int, scalar: Float) {
        checkNaN(scalar)
        val addr = toAddr(x, y)
        array.setFloat(addr + 0, (array.getFloat(addr + 0) * scalar))
        array.setFloat(addr + 1, (array.getFloat(addr + 1) * scalar))
        array.setFloat(addr + 2, (array.getFloat(addr + 2) * scalar))
        array.setFloat(addr + 3, (array.getFloat(addr + 3) * scalar))
    }

    fun forAllMulAssign(scalar: Float) {
        for (i in 0 until TOTAL_SIZE_IN_BYTES / 4) {
            array.setFloat(i, array.getFloat(i) * scalar)
        }
    }

    fun forAllMulAssign(vector: Cvec) {
        for (i in 0 until TOTAL_SIZE_IN_BYTES / 4 step 4) {
            for (k in 0 until 4) {
                array.setFloat(i + 4*k, array.getFloat(i + k) * vector.lane(k))
            }
        }
    }

    fun destroy() = this.array.destroy()

    private inline fun checkNaN(vec: Cvec) {
//        if (vec.r.isNaN() || vec.g.isNaN() || vec.b.isNaN() || vec.a.isNaN()) throw Error("Vector contains NaN (${vec.r},${vec.g},${vec.b},${vec.a})")
    }
    private inline fun checkNaN(scalar: Float) {
//        if (scalar.isNaN()) throw Error("Scalar value is NaN ($scalar)")
    }
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
        setR(x, y, kotlin.math.max(getR(x, y), other.r))
        setG(x, y, kotlin.math.max(getG(x, y), other.g))
        setB(x, y, kotlin.math.max(getB(x, y), other.b))
        setA(x, y, kotlin.math.max(getA(x, y), other.a))
    }
    inline fun mul(x: Int, y: Int, scalar: Float) {
        setR(x, y, getR(x, y) * scalar)
        setG(x, y, getG(x, y) * scalar)
        setB(x, y, getB(x, y) * scalar)
        setA(x, y, getA(x, y) * scalar)
    }

    fun destroy() = {}

}