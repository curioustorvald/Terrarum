package com.sudoplay.joise.generator

/**
 * Created by SKYHi14 on 2017-02-20.
 */
class Xorshift128plus : BasePRNG() {

    private var s0: Long = 0
    private var s1: Long = 0

    init {
        setSeed(10000L)
    }

    override fun get(): Int {
        var x = s0
        val y = s1
        s0 = y
        x = x xor (x shl 23)
        s1 = x xor y xor (x ushr 17) xor (y ushr 26)
        return (s1 + y).toInt()
    }

    override fun setSeed(seed: Long) {
        if (seed == 0L)
            throw IllegalArgumentException("Invalid seed: cannot be zero")

        s0 = (6364136223846793005L * seed + 1442695040888963407L)
        s1 = (6364136223846793005L * s0   + 1442695040888963407L)
    }
}