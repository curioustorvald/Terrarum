package net.torvald.terrarum.ui

import net.torvald.terrarum.gameactors.roundInt
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import java.util.*

/**
 * Image gallery. Images will be equally spaced, counted from top-left to bottom-right.
 * Created by minjaesong on 16-08-08.
 */
class ItemImageGallery(
        override var posX: Int,
        override var posY: Int,
        val width: Int,
        val height: Int,
        val imageList: ArrayList<Image>,
        val column: Int = 1
) : UIItem {

    override fun update(gc: GameContainer, delta: Int) {
    }

    override fun render(gc: GameContainer, g: Graphics) {
        fun column(i: Int) = i % column
        fun row(i: Int) = i / column

        fun imagePosY(i: Int): Int {
            val gutter = (height - imageList[i].height.times(imageList.size)).toFloat().div(
                    imageList.size + 1f
            )
            return row((gutter * i.plus(1) + imageList[i].height * i).roundInt())
        }

        imageList.forEachIndexed { i, image ->
            DrawUtil.drawCentered(g, image,
                    imagePosY(i),
                    width.toFloat().div(column).times(column(i).plus(1)).roundInt(),
                    posX, posY
            )
        }
    }

    override fun keyPressed(key: Int, c: Char) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keyReleased(key: Int, c: Char) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseWheelMoved(change: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}