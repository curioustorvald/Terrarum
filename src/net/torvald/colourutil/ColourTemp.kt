package net.torvald.colourutil

import net.torvald.terrarum.getPixel
import net.torvald.terrarum.weather.toColor
import org.newdawn.slick.Color
import org.newdawn.slick.Image
import net.torvald.colourutil.CIEXYZUtil.toColor

/**
 * RGB-modeled CCT calculator
 * Created by minjaesong on 16-07-26.
 */
object ColourTemp {
    private var envOverlayColourmap = Image("./assets/graphics/colourmap/black_body_col_1000_40000_K.png")

    private fun colTempToImagePos(K: Int): Int {
        if (K < 1000 || K >= 40000) throw IllegalArgumentException("K: out of range. ($K)")
        return (K - 1000) / 10
    }

    /** returns sRGB-normalised colour */
    operator fun invoke(temp: Int): Color =
            envOverlayColourmap.getPixel(colTempToImagePos(temp), 0).toColor()

    /** returns CIExyY-based colour converted to slick.color
     * @param CIE_Y 0.0 - 1.0+ */
    operator fun invoke(temp: Float, CIE_Y: Float): Color =
            CIEXYZUtil.colourTempToXYZ(temp, CIE_Y).toColor()
}