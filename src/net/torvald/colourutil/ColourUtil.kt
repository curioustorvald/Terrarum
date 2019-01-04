package net.torvald.colourutil

import com.badlogic.gdx.graphics.Color
import com.jme3.math.FastMath
import net.torvald.colourutil.CIEXYZUtil.linearise

/**
 * Created by minjaesong on 2016-07-26.
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

    /** Get luminosity level using CIEXYZ colour space. Slow but accurate. */
    fun RGB.getLuminosity(): Float {
        val new = this.linearise()
        return 0.2126729f * new.r + 0.7151522f * new.g + 0.0721750f * new.b // from RGB.toXYZ
    }
    /** Get luminosity level using CIEXYZ colour space. Slow but accurate. */
    fun Color.getLuminosity() = RGB(this).getLuminosity()

    /** Get luminosity level using NTSC standard. Fast, less accurate but should be good enough. */
    fun RGB.getLuminosityQuick() = 0.3f * this.r + 0.59f * this.g + 0.11f * this.b // NTSC standard
    /** Get luminosity level using NTSC standard. Fast, less accurate but should be good enough. */
    fun Color.getLuminosityQuick() = 0.3f * this.r + 0.59f * this.g + 0.11f * this.b // NTSC standard
}