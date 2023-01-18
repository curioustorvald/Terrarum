package net.torvald.gdx.graphics

import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.FloatVector.SPECIES_128
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by minjaesong on 2023-01-18.
 */
class VectorArray(val width: Int, val height: Int) {

    companion object {
        val SPECIES = SPECIES_128
        val BO = ByteOrder.nativeOrder()
        val NULLVEC = FloatVector.broadcast(VectorArray.SPECIES, 0f)
    }

    val TOTAL_SIZE_IN_BYTES = 16 * (width + 1) * (height + 1)
    private val array = ByteBuffer.allocateDirect(TOTAL_SIZE_IN_BYTES)

    private inline fun toAddr(x: Int, y: Int) = 4 * (y * width + x)


    init {
        array.clear()
        array.rewind()
    }

    private fun _getVec(a: Int) = FloatVector.fromByteBuffer(SPECIES, array, a, BO)

    fun getVec(x: Int, y: Int): FloatVector {
        val a = toAddr(x, y)
        return FloatVector.fromByteBuffer(SPECIES, array, a, BO)
    }

    private fun _setVec(a: Int, value: FloatVector) {
        value.intoByteBuffer(array, a, BO)
    }

    fun setVec(x: Int, y: Int, value: FloatVector) {
        val a = toAddr(x, y)
        value.intoByteBuffer(array, a, BO)
    }

    fun setScalar(x: Int, y: Int, value: Float) {
        val a = toAddr(x, y)

        array.putFloat(a + 0, value)
        array.putFloat(a + 1, value)
        array.putFloat(a + 2, value)
        array.putFloat(a + 3, value)
    }

    fun max(x: Int, y: Int, other: FloatVector) {
        val a = toAddr(x, y)
        val mv = _getVec(a).max(other)
        _setVec(a, mv)
    }

    fun mul(x: Int, y: Int, scalar: Float) {
        val a = toAddr(x, y)
        val mv = _getVec(a).max(scalar)
        _setVec(a, mv)
    }

}