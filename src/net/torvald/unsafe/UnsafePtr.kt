package net.torvald.unsafe

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.printStackTrace
import sun.misc.Unsafe
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Further read:
 * - http://www.docjar.com/docs/api/sun/misc/Unsafe.html
 *
 * Created by minjaesong on 2019-06-21.
 */

internal object UnsafeHelper {
    var unsafeAllocatedSize = 0L
        internal set

    val unsafe: Unsafe

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

        printdbg(this, "Allocating 0x${ptr.toString(16)} with size $size, called by:")
        printStackTrace(this)

        return UnsafePtr(ptr, size)
    }

    fun memcpy(src: UnsafePtr, fromIndex: Long, dest: UnsafePtr, toIndex: Long, copyLength: Long) =
            unsafe.copyMemory(src.ptr + fromIndex, dest.ptr + toIndex, copyLength)
    fun memcpy(srcAddress: Long, destAddress: Long, copyLength: Long) =
            unsafe.copyMemory(srcAddress, destAddress, copyLength)
    fun memcpyRaw(srcObj: Any?, srcPos: Long, destObj: Any?, destPos: Long, len: Long) =
            unsafe.copyMemory(srcObj, srcPos, destObj, destPos, len)

    /**
     * The array object in JVM is stored in this memory map:
     *
     * 0                 w                  2w                    *
     * | Some identifier | Other identifier | the actual data ... |
     *
     * (where w = 4 for 32-bit JVM and 8 for 64-bit JVM. If Compressed-OOP is involved, things may get complicated)
     *
     * @return offset from the array's base memory address (aka pointer) that the actual data begins.
     */
    fun getArrayOffset(obj: Any) = unsafe.arrayBaseOffset(obj.javaClass).toLong()
}

/**
 * To allocate a memory, use UnsafeHelper.allocate(long)
 *
 * All the getFloat/Int/whatever methods will follow the endianness of your system,
 * e.g. it'll be Little Endian on x86, Big Endian on PPC, User-defined on ARM; therefore these functions should not be
 * used when the portability matters (e.g. Savefile). In such situations, do byte-wise operations will be needed.
 *
 * Use of hashCode() is forbidden, use the pointer instead.
 */
internal class UnsafePtr(pointer: Long, allocSize: Long) {
    init {
        UnsafeHelper.unsafeAllocatedSize += allocSize
    }

    var destroyed = false
        private set

    var ptr: Long = pointer
        private set

    var size: Long = allocSize
        private set

    fun realloc(newSize: Long) {
        UnsafeHelper.unsafeAllocatedSize -= size
        ptr = UnsafeHelper.unsafe.reallocateMemory(ptr, newSize)
        size = newSize
        UnsafeHelper.unsafeAllocatedSize += size
    }

    fun destroy() {
        if (!destroyed) {
            printdbg(this, "Destroying pointer $this; called from:")
            printStackTrace(this)

            UnsafeHelper.unsafe.freeMemory(ptr)

            destroyed = true

            UnsafeHelper.unsafeAllocatedSize -= size
        }
        else {
            printdbg(this, "Destroy() is called but the pointer $this is already been destroyed; called from:")
            printStackTrace(this)
        }
    }

    private inline fun checkNullPtr(index: Long) { // ignore what IDEA says and do inline this
        // commenting out because of the suspected (or minor?) performance impact.
        // You may break the glass and use this tool when some fucking incomprehensible bugs ("vittujen vitun bugit")
        // appear (e.g. getting garbage values when it fucking shouldn't)

        assert(!destroyed) { throw NullPointerException("The pointer is already destroyed ($this)") }
        assert(index in 0 until size) { throw IndexOutOfBoundsException("Index: $index; alloc size: $size") }
    }

    operator fun get(index: Long): Byte {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getByte(ptr + index)
    }

    operator fun set(index: Long, value: Byte) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putByte(ptr + index, value)
    }

    // NOTE: get/set multibyte values are NOT BYTE-ALIGNED!

    fun getDouble(index: Long): Double {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getDouble(ptr + index)
    }

    fun getLong(index: Long): Long {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getLong(ptr + index)
    }

    fun getFloat(index: Long): Float {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getFloat(ptr + index)
    }

    fun getInt(index: Long): Int {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getInt(ptr + index)
    }

    fun getShort(index: Long): Short {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getShort(ptr + index)
    }

    fun getChar(index: Long): Char {
        checkNullPtr(index)
        return UnsafeHelper.unsafe.getChar(ptr + index)
    }



    fun setDouble(index: Long, value: Double) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putDouble(ptr + index, value)
    }

    fun setLong(index: Long, value: Long) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putLong(ptr + index, value)
    }

    fun setFloat(index: Long, value: Float) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putFloat(ptr + index, value)
    }

    fun setInt(index: Long, value: Int) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putInt(ptr + index, value)
    }

    fun setShort(index: Long, value: Short) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putShort(ptr + index, value)
    }

    fun setChar(index: Long, value: Char) {
        checkNullPtr(index)
        UnsafeHelper.unsafe.putChar(ptr + index, value)
    }



    fun fillWith(byte: Byte) {
        UnsafeHelper.unsafe.setMemory(ptr, size, byte)
    }

    override fun toString() = "0x${ptr.toString(16)} with size $size"
    override fun equals(other: Any?) = this.ptr == (other as UnsafePtr).ptr && this.size == other.size
}

internal class UnsafePtrInputStream(val ptr: UnsafePtr): InputStream() {
    private var p = 0L

    override fun reset() {
        p = 0L
    }

    override fun read(): Int {
        if (p < ptr.size) {
            p += 1
            return ptr[p - 1].toInt().and(255)
        }
        else return -1
    }
}

internal class UnsafePtrOutputStream(val ptr: UnsafePtr): OutputStream() {
    private var p = 0L

    override fun write(p0: Int) {
        if (p < ptr.size) {
            p += 1
            ptr[p - 1] = p0.toByte()
        }
        else throw IOException("Buffer overflow: $p for allocated size ${ptr.size}")
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (p + len >= ptr.size) throw IOException("Buffer overflow: ${p+len} for allocated size ${ptr.size}")
        UnsafeHelper.unsafe.copyMemory(b, off.toLong(), null, ptr.ptr + p, len.toLong())
        p += len
    }
}