package net.torvald.terrarum.mapgenerator

import com.jme3.math.FastMath

/**
 * Quadratic polynomial
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
 *
 * Created by minjaesong on 16-03-31.
 */
object NoiseFilterQuadratic : NoiseFilter {
    override fun getGrad(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = FastMath.pow(FastMath.sqr((1 - WorldGenerator.TERRAIN_AVERAGE_HEIGHT).toFloat()), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                             (start - end) / FastMath.sqr(WorldGenerator.HEIGHT.toFloat()) *
                             FastMath.sqr((func_argX - WorldGenerator.HEIGHT).toFloat()) + end

        if (func_argX < WorldGenerator.TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= WorldGenerator.HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }
}