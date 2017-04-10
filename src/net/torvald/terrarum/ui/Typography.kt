package net.torvald.terrarum.ui

import net.torvald.terrarum.Terrarum
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-07-06.
 */
object Typography {
    fun printCentered(g: Graphics, string: String, screenPosY: Int, ui: UICanvas? = null) {
        val stringW = g.font.getWidth(string)
        val targetW = if (ui == null) Terrarum.WIDTH else ui.width

        g.drawString(string, targetW.minus(stringW).ushr(1).toFloat(), screenPosY.toFloat())
    }

    fun printCentered(g: Graphics, string: String, posX: Int, posY: Int, frameWidth: Int) {
        val stringW = g.font.getWidth(string)

        g.drawString(string, frameWidth.minus(stringW).ushr(1).toFloat() + posX, posY.toFloat())
    }
}