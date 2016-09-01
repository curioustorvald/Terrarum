package net.torvald.colourutil

import com.jme3.math.FastMath
import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-07-26.
 */
object ColourUtil {
    fun toSlickColor(r: Int, g: Int, b: Int) = Color(r.shl(16) or g.shl(8) or b)

    /**
     * Use CIELabUtil.getGradient for natural-looking colour
     */
    fun getGradient(scale: Float, fromCol: Color, toCol: Color): Color {
        val r = FastMath.interpolateLinear(scale, fromCol.r, toCol.r)
        val g = FastMath.interpolateLinear(scale, fromCol.g, toCol.g)
        val b = FastMath.interpolateLinear(scale, fromCol.b, toCol.b)
        val a = FastMath.interpolateLinear(scale, fromCol.a, toCol.a)

        return Color(r, g, b, a)
    }
}