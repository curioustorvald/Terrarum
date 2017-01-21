package net.torvald.terrarum.mapgenerator

import com.jme3.math.FastMath

/**
 * Double Quadratic polynomial
 * (16/9) * (start-end)/height^2 * (x-height)^2 + end
 * 16/9: terrain is formed from 1/4 of height.
 * 1 - (1/4) = 3/4, reverse it and square it.
 * That makes 16/9.

 * Shape:

 * cavity -
 * small
 * -
 * -
 * --
 * ----
 * cavity          --------
 * large                  ----------------

 * @param func_argX
 * *
 * @param start
 * *
 * @param end
 * *
 * @return
 * Created by minjaesong on 16-03-31.
 */
object NoiseFilterCubic : NoiseFilter {
    override fun getGrad(func_argX: Int, start: Double, end: Double): Double {
        val graph_gradient = -FastMath.pow(FastMath.pow((1 - WorldGenerator.TERRAIN_AVERAGE_HEIGHT).toFloat(), 3f), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                             (start - end) / FastMath.pow(WorldGenerator.HEIGHT.toFloat(), 3f) *
                             FastMath.pow((func_argX - WorldGenerator.HEIGHT).toFloat(), 3f) + end

        if (func_argX < WorldGenerator.TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= WorldGenerator.HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }
}