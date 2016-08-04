package net.torvald.terrarum.ui

import net.torvald.terrarum.Terrarum
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 16-08-04.
 */
object DrawUtil {
    fun drawCentered(g: Graphics, image: Image, screenPosY: Int, ui: UICanvas? = null) {
        val imageW = image.width
        val targetW = if (ui == null) Terrarum.WIDTH else ui.width

        g.drawImage(image, targetW.minus(imageW).ushr(1).toFloat(), screenPosY.toFloat())
    }
}