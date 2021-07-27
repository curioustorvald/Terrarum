package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.fillRect
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Created by minjaesong on 2016-08-04.
 */
object Toolkit {

    val DEFAULT_BOX_BORDER_COL = Color(1f, 1f, 1f, 0.2f)

    init {
        CommonResourcePool.addToLoadingList("toolkit_box_border") {
            TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/box_border_flat_tileable.tga"), 1, 1)
        }
        CommonResourcePool.loadAll()
    }

    fun drawCentered(batch: SpriteBatch, image: Texture, screenPosY: Int, ui: UICanvas? = null) {
        val imageW = image.width
        val targetW = if (ui == null) AppLoader.screenSize.screenW else ui.width

        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat(), screenPosY.toFloat())
    }

    fun drawCentered(batch: SpriteBatch, image: Texture, screenPosY: Int, targetW: Int, offsetX: Int = 0, offsetY: Int = 0) {
        val imageW = image.width
        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat() + offsetX, screenPosY.toFloat() + offsetY)
    }

    fun fillArea(batch: SpriteBatch, x: Int, y: Int, w: Int, h: Int) {
        batch.fillRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }

    /**
     * Parameters are THAT OF THE BOX, the border will be drawn OUTSIDE of the params you specified!
     */
    fun drawBoxBorder(batch: SpriteBatch, x: Int, y: Int, w: Int, h: Int) {
        val pack = CommonResourcePool.getAsTextureRegionPack("toolkit_box_border")
        val tx = pack.tileW.toFloat()
        val ty = pack.tileH.toFloat()

        // top edge
        batch.draw(pack.get(1, 0), x.toFloat(), y - ty, w.toFloat(), ty)
        // bottom edge
        batch.draw(pack.get(1, 2), x.toFloat(), y.toFloat() + h, w.toFloat(), ty)
        // left edge
        batch.draw(pack.get(0, 1), x.toFloat() - tx, y.toFloat(), tx, h.toFloat())
        // right edge
        batch.draw(pack.get(2, 1), x.toFloat() + w, y.toFloat(), tx, h.toFloat())

        // top left point
        /*batch.draw(pack.get(0, 0), x - tx, y - ty)
        // top right point
        batch.draw(pack.get(2, 0), x + tx, y - ty)
        // bottom left point
        batch.draw(pack.get(0, 2), x - tx, y + ty)
        // bottom right point
        batch.draw(pack.get(2, 2), x + tx, y + ty)*/

    }

}