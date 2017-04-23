package net.torvald.colourutil

import net.torvald.terrarum.getPixel
import net.torvald.terrarum.weather.toColor
import org.newdawn.slick.Color
import org.newdawn.slick.Image
import net.torvald.colourutil.CIEXYZUtil.toColor
import net.torvald.terrarum.ModMgr

/**
 * RGB- and CIE-Modeled CCT calculator
 * Created by minjaesong on 16-07-26.
 */
object ColourTemp {
    private var clut = Image(ModMgr.getPath("basegame", "colourmap/black_body_col_1000_40000_K.tga"))

    private fun colTempToImagePos(K: Int): Int {
        if (K < 1000 || K >= 40000) throw IllegalArgumentException("K: out of range. ($K)")
        return (K - 1000) / 10
    }

    /** returns sRGB-normalised colour */
    operator fun invoke(temp: Int): Color =
            clut.getPixel(colTempToImagePos(temp), 0).toColor()

    /** returns CIExyY-based colour converted to slick.color
     * @param CIE_Y 0.0 - 1.0+ */
    operator fun invoke(temp: Float, CIE_Y: Float): Color =
            CIEXYZUtil.colourTempToXYZ(temp, CIE_Y).toColor()
}