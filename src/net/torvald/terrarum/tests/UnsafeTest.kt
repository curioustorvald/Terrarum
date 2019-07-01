package net.torvald.terrarum.tests

import net.torvald.UnsafeHelper
import net.torvald.terrarum.gameworld.toUint
import sun.misc.Unsafe

/**
 * Created by minjaesong on 2019-06-22.
 */
class UnsafeTest {

    private val unsafe: Unsafe
    init {
        val unsafeConstructor = Unsafe::class.java.getDeclaredConstructor()
        unsafeConstructor.isAccessible = true
        unsafe = unsafeConstructor.newInstance()
    }

    private val memsize = 512L // must be big enough value so that your OS won't always return zero-filled pieces

    fun main() {
        var ptr = unsafe.allocateMemory(memsize)
        printDump(ptr)

        unsafe.setMemory(ptr, memsize, 0x00.toByte())
        printDump(ptr)

        for (k in 0 until 13) {
            unsafe.putByte(ptr + k, (-1 - k).toByte())
        }
        printDump(ptr)

        // test shingled memory copy -- how would it work out?
        UnsafeHelper.memcpy(ptr, ptr + 3L, 13L)

        printDump(ptr)


        println(ptr)
        ptr = unsafe.reallocateMemory(ptr, 256L)
        println(ptr)


        // that's all for today!
        unsafe.freeMemory(ptr)
    }


    fun printDump(ptr: Long) {
        println("MINIMINIDUMP START")
        for (i in 0 until memsize) {
            val b = unsafe.getByte(ptr + i).toUint().toString(16).padStart(2, '0')
            print("$b ")
        }
        println("\nMINIMINIDUMP END")
    }

}

fun main(args: Array<String>) {
    UnsafeTest().main()
}