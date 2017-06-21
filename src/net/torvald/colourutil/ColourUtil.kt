package net.torvald.colourutil

import com.jme3.math.FastMath
import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 16-07-26.
 */
object ColourUtil {
    fun toColor(r: Int, g: Int, b: Int) = Color(r.shl(24) or g.shl(16) or b.shl(8) or 0xff)

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