package net.torvald.terrarum.worldgenerator

import com.jme3.math.FastMath

/**
 * Created by minjaesong on 16-03-31.
 */
object NoiseFilterSqrt : NoiseFilter {
    override fun getGrad(func_argX: Int, start: Double, end: Double): Double {
        val graph_gradient = (end - start) / FastMath.sqrt((WorldGenerator.HEIGHT - WorldGenerator.TERRAIN_AVERAGE_HEIGHT).toFloat()) * FastMath.sqrt((func_argX - WorldGenerator.TERRAIN_AVERAGE_HEIGHT).toFloat()) + start

        if (func_argX < WorldGenerator.TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= WorldGenerator.HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }
}