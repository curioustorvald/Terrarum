package net.torvald.terrarum.mapgenerator

/**
 * Created by minjaesong on 16-03-31.
 */
object NoiseFilterUniform : NoiseFilter {
    override fun getGrad(func_argX: Int, start: Double, end: Double): Double {
        return 1.0
    }
}