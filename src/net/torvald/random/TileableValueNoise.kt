package net.torvald.random

/**
 * Generate value noise that is always "tileably looped" every x in loopSize.
 *
 * Created by minjaesong on 16-10-28.
 */
class TileableValueNoise(
        val octaves: Int, val persistency: Double,
        val loopSize: Double = 1.0, val seed: Long? = null
) : NoiseGenerator1D {

    val rng = if (seed != null) HQRNG(seed) else HQRNG()

    override fun get(x: Double): Double {
        TODO()
    }
}