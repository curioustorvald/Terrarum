package net.torvald.colourutil

import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-07-26.
 */
object ColourUtil {
    fun toSlickColor(r: Int, g: Int, b: Int) = Color(r.shl(16) or g.shl(8) or b)
}