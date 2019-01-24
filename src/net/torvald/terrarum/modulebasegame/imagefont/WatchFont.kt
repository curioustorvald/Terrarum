package net.torvald.terrarum.modulebasegame.imagefont

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.ModMgr
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-01-24.
 */
object WatchFont : BitmapFont() {
    internal val W = 9
    internal val H = 12

    internal val fontSheet = TextureRegionPack(ModMgr.getGdxFile("basegame", "fonts/watch_new.tga"), W, H)

    init {
        setOwnsTexture(true)
    }

    override fun draw(batch: Batch, str: CharSequence, x: Float, y: Float): GlyphLayout? {

        str.forEachIndexed { index, c ->
            batch.draw(
                    fontSheet.get((c - '0') % 16, (c - '0') / 16),
                    x + W * index, y
            )
        }


        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()
}