package net.torvald.gdx.graphics

import jdk.incubator.vector.FloatVector
import net.torvald.unsafe.UnsafeHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

//    fun isDestroyed() = array.destroyed

    private val byteOrder = ByteOrder.nativeOrder()

    init {
        zerofill()
    }

    // getters
    fun getR(x: Int, y: Int) = array.getFloat(toAddr(x, y))
    fun getG(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 1)
    fun getB(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 2)
    fun getA(x: Int, y: Int) = array.getFloat(toAddr(x, y) + 3)
    inline fun getVec(x: Int, y: Int) = Cvec(getFloatVector(x, y))
    inline fun getFloatVector(x: Int, y: Int): FloatVector {
        val offset = toAddr(x, y)
        val array = floatArrayOf(
                array.getFloat(offset),
                array.getFloat(offset + 1),
                array.getFloat(offset + 2),
                array.getFloat(offset + 3)
        )
        return FloatVector.fromArray(FloatVector.SPECIES_128, array, 0)
    }
    // setters
    fun zerofill() {
        array.fillWith(0)
    }
//    fun setR(x: Int, y: Int, value: Float) { array.putFloat(toAddr(x, y), value) }
//    fun setG(x: Int, y: Int, value: Float) { array.putFloat(toAddr(x, y) + 1, value) }
//    fun setB(x: Int, y: Int, value: Float) { array.putFloat(toAddr(x, y) + 2, value) }
//    fun setA(x: Int, y: Int, value: Float) { array.putFloat(toAddr(x, y) + 3, value) }
    inline fun setVec(x: Int, y: Int, value: Cvec) {
        setFromFloatVector(x, y, value.vec)
    }

    inline fun setFromFloatVector(x: Int, y: Int, value: FloatVector) {
        val offset = toAddr(x, y)
        value.toArray().forEachIndexed { index, fl ->
            array.setFloat(offset + index, fl)
        }
    }

    // operators
    inline fun max(x: Int, y: Int, other: Cvec) {
        setFromFloatVector(x, y, getFloatVector(x, y).max(other.vec))
    }
    inline fun mul(x: Int, y: Int, scalar: Float) {
        setFromFloatVector(x, y, getFloatVector(x, y).mul(scalar))
    }

    /*fun mulAndAssign(x: Int, y: Int, scalar: Float) {
        val addr = toAddr(x, y)
        for (k in 0..3) {
            array.putFloat(addr + k, (array.getFloat(addr + k) * scalar))
        }
    }

    fun forAllMulAssign(scalar: Float) {
        for (i in 0 until TOTAL_SIZE_IN_BYTES / 4) {
            array.putFloat(i, array.getFloat(i) * scalar)
        }
    }

    fun forAllMulAssign(vector: Cvec) {
        for (i in 0 until TOTAL_SIZE_IN_BYTES / 4 step 4) {
            for (k in 0 until 4) {
                array.putFloat(i + 4*k, array.getFloat(i + k) * vector.getElem(k))
            }
        }
    }*/

    fun destroy() = this.array.destroy()

}
