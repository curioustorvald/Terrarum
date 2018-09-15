package net.torvald.random

import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.serialise.toLittleLong
import org.apache.commons.codec.digest.DigestUtils
import java.util.Random

/**
 * Xorshift128+
 */
class HQRNG @JvmOverloads constructor(seed: Long = System.nanoTime()) : Random() {

    private var s0: Long
    private var s1: Long

    constructor(s0: Long, s1: Long) : this() {
        this.s0 = s0
        this.s1 = s1
    }

    init {
        if (seed == 0L)
            throw IllegalArgumentException("Invalid seed: cannot be zero")

        val hash = DigestUtils.sha256(seed.toString())

        s0 = hash.copyOfRange(0, 8).toLittleLong()
        s1 = hash.copyOfRange(8, 16).toLittleLong()
    }

    override fun nextLong(): Long {
        var x = s0
        val y = s1
        s0 = y
        x = x xor (x shl 23)
        s1 = x xor y xor (x ushr 17) xor (y ushr 26)
        return s1 + y
    }

    fun serialize() = s0.toLittle() + s1.toLittle()

    fun reseed(s0: Long, s1: Long) {
        this.s0 = s0
        this.s1 = s1
    }
}