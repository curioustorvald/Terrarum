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
    override fun getGrad(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = -FastMath.pow(FastMath.pow((1 - MapGenerator.TERRAIN_AVERAGE_HEIGHT).toFloat(), 3f), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                             (start - end) / FastMath.pow(MapGenerator.HEIGHT.toFloat(), 3f) *
                             FastMath.pow((func_argX - MapGenerator.HEIGHT).toFloat(), 3f) + end

        if (func_argX < MapGenerator.TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= MapGenerator.HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }
}