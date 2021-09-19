package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Created by minjaesong on 2016-08-04.
 */
object Toolkit : Disposable {

    val DEFAULT_BOX_BORDER_COL = Color(1f, 1f, 1f, 0.2f)

    private val shaderBlur = App.loadShaderFromFile("assets/blur.vert", "assets/blur2.frag")
    val baloonTile = TextureRegionPack("assets/graphics/gui/message_black_tileable.tga", 36, 36)


    init {
        App.disposableSingletonsPool.add(this)

        CommonResourcePool.addToLoadingList("toolkit_box_border") {
            TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/box_border_flat_tileable.tga"), 1, 1)
        }
        CommonResourcePool.loadAll()
    }


    override fun dispose() {
        baloonTile.dispose()
    }


    fun drawCentered(batch: SpriteBatch, image: Texture, screenPosY: Int, ui: UICanvas? = null) {
        val imageW = image.width
        val targetW = if (ui == null) App.scr.width else ui.width

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

    fun blurEntireScreen(batch: SpriteBatch, camera: OrthographicCamera, blurRadius: Float, x: Int, y: Int, w: Int, h: Int) {
        for (i in 0 until 6) {
            val scalar = blurRadius * (1 shl i.ushr(1))

            batch.shader = shaderBlur
            shaderBlur.setUniformMatrix("u_projTrans", camera.combined)
            shaderBlur.setUniformi("u_texture", 0)
            shaderBlur.setUniformf("iResolution", w.toFloat(), h.toFloat())
            IngameRenderer.shaderBlur.setUniformf("flip", 1f)
            if (i % 2 == 0)
                IngameRenderer.shaderBlur.setUniformf("direction", scalar, 0f)
            else
                IngameRenderer.shaderBlur.setUniformf("direction", 0f, scalar)

            val p = Pixmap.createFromFrameBuffer(0, 0, w, h)
            val t = Texture(p); t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

            batch.draw(t, 0f, 0f)
            batch.flush() // so I can safely dispose of the texture

            t.dispose(); p.dispose()
        }

        batch.shader = null
    }

    fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) {
        // centre area
        batch.draw(baloonTile.get(1, 1), x, y, w, h)

        // edges
        batch.draw(baloonTile.get(1, 0), x, y - baloonTile.tileH, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(1, 2), x, y + h, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(0, 1), x - baloonTile.tileW, y, baloonTile.tileW.toFloat(), h)
        batch.draw(baloonTile.get(2, 1), x + w, y, baloonTile.tileW.toFloat(), h)

        // corners
        batch.draw(baloonTile.get(0, 0), x - baloonTile.tileW, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 0), x + w, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 2), x + w, y + h)
        batch.draw(baloonTile.get(0, 2), x - baloonTile.tileW, y + h)
    }

}