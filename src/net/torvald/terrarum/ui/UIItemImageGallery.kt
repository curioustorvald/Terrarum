package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.roundInt
import java.util.*

/**
 * Image gallery. Images will be equally spaced, counted from top-left to bottom-right.
 * Created by minjaesong on 2016-08-08.
 */
class UIItemImageGallery(
        parentUI: UICanvas,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        override val height: Int,
        val imageList: ArrayList<Texture>,
        val column: Int = 1
) : UIItem(parentUI) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    override fun update(delta: Float) {
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        fun column(i: Int) = i % column
        fun row(i: Int) = i / column

        fun imagePosY(i: Int): Int {
            val gutter = (height - imageList[i].height.times(imageList.size)).toFloat().div(
                    imageList.size + 1f
            )
            return row((gutter * i.plus(1) + imageList[i].height * i).roundInt())
        }

        imageList.forEachIndexed { i, image ->
            Toolkit.drawCentered(batch, image,
                    imagePosY(i),
                    width.toFloat().div(column).times(column(i).plus(1)).roundInt(),
                    posX, posY
            )
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keyUp(keycode: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun scrolled(amount: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dispose() {
        imageList.forEach { it.dispose() }
    }
}