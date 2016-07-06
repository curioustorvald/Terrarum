package net.torvald.terrarum.ui

import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-07-06.
 */
object Typesetter {
    fun printCentered(string: String, screenPosY: Int, ui: UICanvas, g: Graphics) {
        val stringW = g.font.getWidth(string)
        val targetW = ui.width

        g.drawString(string, targetW.minus(stringW).ushr(1).toFloat(), screenPosY.toFloat())
    }
}