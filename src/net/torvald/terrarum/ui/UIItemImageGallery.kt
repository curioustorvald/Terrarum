package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import java.util.*
import kotlin.math.roundToInt

/**
 * Image gallery. Images will be equally spaced, counted from top-left to bottom-right.
 * Created by minjaesong on 2016-08-08.
 */
class UIItemImageGallery(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override val height: Int,
        val imageList: ArrayList<Texture>,
        val column: Int = 1
) : UIItem(parentUI, initialX, initialY) {

    override fun update(delta: Float) {
    }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        fun column(i: Int) = i % column
        fun row(i: Int) = i / column

        fun imagePosY(i: Int): Int {
            val gutter = (height - imageList[i].height.times(imageList.size)).toFloat().div(
                    imageList.size + 1f
            )
            return row((gutter * i.plus(1) + imageList[i].height * i).roundToInt())
        }

        imageList.forEachIndexed { i, image ->
            Toolkit.drawCentered(batch, image,
                    imagePosY(i),
                    width.toFloat().div(column).times(column(i).plus(1)).roundToInt(),
                    posX, posY
            )
        }
    }

    override fun dispose() {
        imageList.forEach { it.dispose() }
    }
}