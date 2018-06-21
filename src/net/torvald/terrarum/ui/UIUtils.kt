package net.torvald.terrarum.ui

import net.torvald.terrarum.sqr

/**
 * Created by minjaesong on 2017-03-14.
 */
object UIUtils {
    fun moveQuick(start: Double, end: Double, timer: Double, duration: Double) =
            (start - end) * ((timer / duration) - 1).sqr() + end
}