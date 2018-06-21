package net.torvald.terrarum.modulebasegame.worldgenerator

/**
 * Created by minjaesong on 2016-03-31.
 */
object NoiseFilterUniform : NoiseFilter {
    override fun getGrad(func_argX: Int, start: Double, end: Double): Double {
        return 1.0
    }
}