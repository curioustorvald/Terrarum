package net.torvald.terrarum.imagefont

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.ModMgr
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-21.
 */
object WatchDotAlph : BitmapFont() {
    val charMapping = ('A'..'Z')

    internal val W = 12
    internal val H = 10

    internal val fontSheet = TextureRegionPack(ModMgr.getGdxFile("basegame", "fonts/watch_dotalph.tga"), W, H)

    init {
        setOwnsTexture(true)
    }

    override fun draw(batch: Batch, str: CharSequence, x: Float, y: Float): GlyphLayout? {

        str.forEachIndexed { index, c ->
            batch.draw(
                    if (c != ' ' && c in charMapping)
                        fontSheet.get(1 + (c - 'A'), 0)
                    else
                        fontSheet.get(0, 0)
                    , x + W * index, y)
        }


        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()
}