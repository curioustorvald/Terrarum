package net.torvald.terrarum.ui

import com.jme3.math.FastMath

/**
 * Created by minjaesong on 2016-03-22.
 */
object Movement{
    /**
     * Fast at the beginning, getting slow over time.
     */
    fun fastPullOut(scale: Float, start: Float = 0f, end: Float = 1f): Float =
            if (scale < 0f) start
            else if (scale > 1f) end
            else (start - end) * FastMath.sqr(scale - 1) + end

    /**
     * Slow at the beginning, getting fast over time.
     */
    fun dropDown(scale: Float, start: Float = 0f, end: Float = 1f): Float =
            if (scale < 0f) start
            else if (scale > 1f) end
            else (end - start) * FastMath.sqr(scale) + start

    fun sinusoid(scale: Float, start: Float = 0f, end: Float = 1f): Float =
            if (scale < 0f) start
            else if (scale > 1f) end
            else (start - end) * FastMath.cos2(0.5f * FastMath.PI * scale) + end

}