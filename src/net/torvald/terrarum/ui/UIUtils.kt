package net.torvald.terrarum.ui

import net.torvald.terrarum.sqr

/**
 * Created by minjaesong on 2017-03-14.
 */
object UIUtils {
    fun moveQuick(start: Float, end: Float, timer: Float, duration: Float) =
            (start - end) * ((timer / duration) - 1).sqr() + end
    fun moveLinear(start: Float, end: Float, timer: Float, duration: Float) =
            (start - end) * (1 - (timer / duration)) + end
}