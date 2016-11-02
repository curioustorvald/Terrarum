package net.torvald.random

/**
 * Created by minjaesong on 16-10-28.
 */
interface NoiseGenerator1D {
    fun generate(seed: Long)
    operator fun get(x: Int): Float
}