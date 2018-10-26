/*package net.torvald.random

import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.serialise.toLittleLong
import org.apache.commons.codec.digest.DigestUtils
import java.util.Random

/**
 * Xoroshift128
 *
 * @see https://github.com/SquidPony/SquidLib/blob/master/squidlib-util/src/main/java/squidpony/squidmath/XoRoRNG.java
 */
class HQRNG() : Random() {

    private val DOUBLE_MASK = (1L shl 53) - 1
    private val NORM_53 = 1.0 / (1L shl 53)
    private val FLOAT_MASK = (1L shl 24) - 1
    private val NORM_24 = 1.0 / (1L shl 24)

    var state0: Long = 0L; private set
    var state1: Long = 0L; private set

    /**
     * Creates a new generator seeded using four calls to Math.random().
     */
    init {
        reseed(((Math.random() - 0.5) * 0x10000000000000L).toLong() xor ((Math.random() - 0.5) * 2.0 * -0x8000000000000000L).toLong(),
                ((Math.random() - 0.5) * 0x10000000000000L).toLong() xor ((Math.random() - 0.5) * 2.0 * -0x8000000000000000L).toLong())
    }

    /**
     * Constructs this XoRoRNG by dispersing the bits of seed using [.setSeed] across the two parts of state
     * this has.
     * @param seed a long that won't be used exactly, but will affect both components of state
     */
    constructor(seed: Long): this() {
        setSeed(seed)
    }

    /**
     * Constructs this XoRoRNG by calling [.setSeed] on the arguments as given; see that method for
     * the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    constructor(stateA: Long, stateB: Long): this() {
        reseed(stateA, stateB)
    }

    public override fun next(bits: Int): Int {
        val s0 = state0
        var s1 = state1
        val result = (s0 + s1).toInt().ushr(32 - bits)
        s1 = s1 xor s0
        state0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14) // a, b
        state1 = s1 shl 36 or s1.ushr(28) // c
        return result
    }

    override fun nextLong(): Long {
        val s0 = state0
        var s1 = state1
        val result = s0 + s1

        s1 = s1 xor s0
        state0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14) // a, b
        state1 = s1 shl 36 or s1.ushr(28) // c
        /*
        state0 = Long.rotateLeft(s0, 55) ^ s1 ^ (s1 << 14); // a, b
        state1 = Long.rotateLeft(s1, 36); // c
        */
        return result
    }

    /**
     * Exclusive on the outer bound; the inner bound is 0. The bound may be negative, which will produce a non-positive
     * result.
     * @param bound the outer exclusive bound; may be positive or negative
     * @return a random int between 0 (inclusive) and bound (exclusive)
     */
    override fun nextInt(bound: Int): Int {
        return (bound * nextLong().ushr(33) shr 31).toInt()
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param inner the inner bound, inclusive, can be positive or negative
     * @param outer the outer bound, exclusive, should be positive, should usually be greater than inner
     * @return a random int that may be equal to inner and will otherwise be between inner and outer
     */
    fun nextInt(inner: Int, outer: Int): Int {
        return inner + nextInt(outer - inner)
    }

    /**
     * Exclusive on the outer bound; the inner bound is 0. The bound may be negative, which will produce a non-positive
     * result.
     * @param bound the outer exclusive bound; may be positive or negative
     * @return a random long between 0 (inclusive) and bound (exclusive)
     */
    fun nextLong(bound: Long): Long {
        var bound = bound
        var rand = nextLong()
        val randLow = rand and 0xFFFFFFFFL
        val boundLow = bound and 0xFFFFFFFFL
        rand = rand ushr 32
        bound = bound shr 32
        val z = randLow * boundLow shr 32
        var t = rand * boundLow + z
        val tLow = t and 0xFFFFFFFFL
        t = t ushr 32
        return rand * bound + t + (tLow + randLow * bound shr 32) - (z shr 63) - (bound shr 63)
    }

    /**
     * Inclusive inner, exclusive outer; both inner and outer can be positive or negative.
     * @param inner the inner bound, inclusive, can be positive or negative
     * @param outer the outer bound, exclusive, can be positive or negative and may be greater than or less than inner
     * @return a random long that may be equal to inner and will otherwise be between inner and outer
     */
    fun nextLong(inner: Long, outer: Long): Long {
        return inner + nextLong(outer - inner)
    }

    override fun nextDouble(): Double {
        return (nextLong() and DOUBLE_MASK) * NORM_53
    }

    override fun nextFloat(): Float {
        return ((nextLong() and FLOAT_MASK) * NORM_24).toFloat()
    }

    override fun nextBoolean(): Boolean {
        return nextLong() < 0L
    }

    override fun nextBytes(bytes: ByteArray) {
        var i = bytes.size
        var n = 0
        while (i != 0) {
            n = Math.min(i, 8)
            var bits = nextLong()
            while (n-- != 0) {
                bytes[--i] = bits.toByte()
                bits = bits ushr 8
            }
        }
    }


    fun serialize() = state0.toLittle() + state1.toLittle()

    /**
     * Sets the seed of this generator using one long, running that through LightRNG's algorithm twice to get the state.
     * @param seed the number to use as the seed
     */
    override fun setSeed(seed: Long) {
        var state = seed + -0x61c8864680b583ebL
        var z = state
        z = (z xor z.ushr(30)) * -0x40a7b892e31b1a47L
        z = (z xor z.ushr(27)) * -0x6b2fb644ecceee15L
        state0 = z xor z.ushr(31)
        state += -0x61c8864680b583ebL
        z = state
        z = (z xor z.ushr(30)) * -0x40a7b892e31b1a47L
        z = (z xor z.ushr(27)) * -0x6b2fb644ecceee15L
        state1 = z xor z.ushr(31)
    }

    /**
     * Sets the seed of this generator using two longs, using them without changes unless both are 0 (then it makes the
     * state variable corresponding to stateA 1 instead).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    fun reseed(stateA: Long, stateB: Long) {

        state0 = stateA
        state1 = stateB
        if (stateA or stateB == 0L)
            state0 = 1L
    }
}*/