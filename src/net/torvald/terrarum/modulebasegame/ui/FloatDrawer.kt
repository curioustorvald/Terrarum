package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.AppLoader
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-05-24.
 */
object FloatDrawer : Disposable {

    val tile = TextureRegionPack("assets/graphics/gui/message_white_tileable.tga", 16, 16)

    init {
        AppLoader.disposableSingletonsPool.add(this)
    }

    /**
     * Draws the Float at given position in given size. The size is that of the centre area, excluding the edges. Size of the edges are 8x8 pixels.
     */
    operator fun invoke(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) {
        // centre area
        batch.draw(tile.get(1, 1), x, y, w, h)

        // edges
        batch.draw(tile.get(1, 0), x, y - tile.tileH, w, tile.tileH.toFloat())
        batch.draw(tile.get(1, 2), x, y + h, w, tile.tileH.toFloat())
        batch.draw(tile.get(0, 1), x - tile.tileW, y, tile.tileW.toFloat(), h)
        batch.draw(tile.get(2, 1), x + w, y, tile.tileW.toFloat(), h)

        // corners
        batch.draw(tile.get(0, 0), x - tile.tileW, y - tile.tileH)
        batch.draw(tile.get(2, 0), x + w, y - tile.tileH)
        batch.draw(tile.get(2, 2), x + w, y + h)
        batch.draw(tile.get(0, 2), x - tile.tileW, y + h)
    }

    override fun dispose() {
        tile.dispose()
    }

}