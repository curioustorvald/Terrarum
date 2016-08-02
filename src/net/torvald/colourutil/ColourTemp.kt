package net.torvald.colourutil

import org.newdawn.slick.Color
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 16-07-26.
 */
object ColourTemp {
    private var envOverlayColourmap = Image("./assets/graphics/colourmap/black_body_col_1000_40000_K.png")

    private fun colTempToImagePos(K: Int): Int {
        if (K < 1000 || K >= 40000) throw IllegalArgumentException("K: out of range. ($K)")
        return (K - 1000) / 10
    }

    operator fun invoke(temp: Int): Color = envOverlayColourmap.getColor(colTempToImagePos(temp), 0)
}