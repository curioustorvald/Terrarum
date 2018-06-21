package net.torvald.terrarum.modulebasegame.imagefont

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.ModMgr
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-21.
 */

object Watch7SegMain : BitmapFont() {
    val charMapping = ('0'..'9')

    internal val W = 11
    internal val H = 18

    internal val fontSheet = TextureRegionPack(ModMgr.getGdxFile("basegame", "fonts/7segnum.tga"), W, H)

    init {
        setOwnsTexture(true)
    }

    override fun draw(batch: Batch, str: CharSequence, x: Float, y: Float): GlyphLayout? {

        str.forEachIndexed { index, c ->
            batch.draw(
                    if (c != ' ' && c in charMapping)
                        fontSheet.get(1 + (c - '0'), 0)
                    else
                        fontSheet.get(0, 0)
                    , x + W * index, y)
        }


        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()

    override fun dispose() {
        fontSheet.dispose()
    }
}