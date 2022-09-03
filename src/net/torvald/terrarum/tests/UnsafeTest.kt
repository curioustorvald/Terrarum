package net.torvald.terrarum.tests

import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
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
        val intarray = intArrayOf(5,4,3,2,1)

        val arrayBaseOffset = UnsafeHelper.getArrayOffset(intarray) // should be 16 or 12 on 64-bit JVM

        println(arrayBaseOffset)
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