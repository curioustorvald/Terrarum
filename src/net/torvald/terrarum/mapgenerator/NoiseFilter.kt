package net.torvald.terrarum.mapgenerator

/**
 * Created by minjaesong on 16-03-31.
 */
interface NoiseFilter {
    fun getGrad(func_argX: Int, start: Double, end: Double): Double
}