package net.torvald.random

import java.util.Random

/**
 * Xorshift128+
 */
class HQRNG @JvmOverloads constructor(seed: Long = System.nanoTime()) : Random() {

    private var s0: Long
    private var s1: Long

    init {
        if (seed == 0L)
            throw IllegalArgumentException("Invalid seed: cannot be zero")

        s0 = (6364136223846793005L * seed + 1442695040888963407L)
        s1 = (6364136223846793005L * s0   + 1442695040888963407L)
    }

    override fun nextLong(): Long {
        var x = s0
        val y = s1
        s0 = y
        x = x xor (x shl 23)
        s1 = x xor y xor (x ushr 17) xor (y ushr 26)
        return s1 + y
    }
}