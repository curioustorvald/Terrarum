package net.torvald.terrarum.worldgenerator

/**
 * Created by minjaesong on 2016-03-31.
 */
interface NoiseFilter {
    fun getGrad(func_argX: Int, start: Double, end: Double): Double
}