package net.torvald

import net.torvald.terrarum.printStackTrace
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

    /**
     * A factory method to allocate a memory of given size and return its starting address as a pointer.
     */
    fun allocate(size: Long): UnsafePtr {
        val ptr = unsafe.allocateMemory(size)
        return UnsafePtr(ptr, size)
    }

    fun memcpy(src: UnsafePtr, fromIndex: Long, dest: UnsafePtr, toIndex: Long, copyLength: Long) {
        // unsafe.copyMemory(srcAddress, destAddress, bytes); in case no src for the sun.misc.Unsafe is available :D
        unsafe.copyMemory(src.ptr + fromIndex, dest.ptr + toIndex, copyLength)
    }

    fun memcpy(srcAddress: Long, destAddress: Long, copyLength: Long) = unsafe.copyMemory(srcAddress, destAddress, copyLength)
}

/**
 * To allocate a memory, use UnsafeHelper.allocate(long)
 *
 * All the getFloat/Int/whatever methods will follow the endianness of your system,
 * e.g. it'll be Little Endian on x86, Big Endian on PPC, User-defined on ARM; therefore these functions should not be
 * used when the portability matters (e.g. Savefile). In such situations, do byte-wise operations will be needed.
 */
class UnsafePtr(pointer: Long, allocSize: Long) {
    var destroyed = false
        private set

    var ptr: Long = pointer
        private set

    var size: Long = allocSize
        private set

    fun realloc(newSize: Long) {
        ptr = UnsafeHelper.unsafe.reallocateMemory(ptr, newSize)
    }

    fun destroy() {
        if (!destroyed) {
            UnsafeHelper.unsafe.freeMemory(ptr)

            println("[UnsafePtr] Destroying pointer $this; called from:")
            printStackTrace(this)

            destroyed = true
        }
    }

    private inline fun checkNullPtr(index: Long) { // ignore what IDEA says and do inline this
        // commenting out because of the suspected (or minor?) performance impact.
        // You may break the glass and use this tool when some fucking incomprehensible bugs ("vittujen vitun bugit")
        // appear (e.g. getting garbage values when it fucking shouldn't)
        assert(destroyed) { throw NullPointerException("The pointer is already destroyed ($this)") }

        // OOB Check: debugging purposes only -- comment out for the production
        //if (index !in 0 until size) throw IndexOutOfBoundsException("Index: $index; alloc size: $size")
    }

    operator fun get(index: Long): Byte {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getByte(ptr + index)
    }

    fun getFloat(index: Long): Float {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getFloat(ptr + index)
    }

    fun getInt(index: Long): Int {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getInt(ptr + index)
    }

    operator fun set(index: Long, value: Byte) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putByte(ptr + index, value)
    }

    fun setFloat(index: Long, value: Float) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putFloat(ptr + index, value)
    }

    fun setInt(index: Long, value: Int) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putInt(ptr + index, value)
    }

    fun fillWith(byte: Byte) {
        UnsafeHelper.unsafe.setMemory(ptr, size, byte)
    }

    override fun toString() = "0x${ptr.toString(16)} with size $size"
    override fun equals(other: Any?) = this.ptr == (other as UnsafePtr).ptr
}