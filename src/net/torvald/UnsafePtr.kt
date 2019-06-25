package net.torvald

import sun.misc.Unsafe

/**
 * Created by minjaesong on 2019-06-21.
 */

object UnsafeHelper {
    internal val unsafe: Unsafe

    init {
        val unsafeConstructor = Unsafe::class.java.getDeclaredConstructor()
        unsafeConstructor.isAccessible = true
        unsafe = unsafeConstructor.newInstance()
    }


    fun allocate(size: Long): UnsafePtr {
        val ptr = unsafe.allocateMemory(size)
        return UnsafePtr(ptr, size)
    }
}

/**
 * To allocate a memory, use UnsafeHelper.allocate(long)
 */
class UnsafePtr(val ptr: Long, val allocSize: Long) {
    var destroyed = false
        private set

    fun destroy() {
        if (!destroyed) {
            UnsafeHelper.unsafe.freeMemory(ptr)

            println("[UnsafePtr] Destroying pointer $this; called from:")
            Thread.currentThread().stackTrace.forEach { println("[UnsafePtr] $it") }

            destroyed = true
        }
    }

    private inline fun checkNullPtr(index: Long) {
        if (destroyed) throw NullPointerException("The pointer is already destroyed ($this)")

        // OOB Check: debugging purposes only -- comment out for the production
        //if (index !in 0 until allocSize) throw IndexOutOfBoundsException("Index: $index; alloc size: $allocSize")
    }

    operator fun get(index: Long): Byte {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getByte(ptr + index)
    }

    fun getFloat(index: Long): Float {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getFloat(ptr + index)
    }

    operator fun set(index: Long, value: Byte) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putByte(ptr + index, value)
    }

    fun setFloat(index: Long, value: Float) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putFloat(ptr + index, value)
    }

    fun fillWith(byte: Byte) {
        UnsafeHelper.unsafe.setMemory(ptr, allocSize, byte)
    }

    override fun toString() = "0x${ptr.toString(16)} with size $allocSize"
}