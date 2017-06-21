package net.torvald.terrarum.imagefont

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.ModMgr
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-21.
 */
object Watch7SegSmall : BitmapFont() {
    val charMapping = (' '..'9')

    internal val W = 9
    internal val H = 12

    internal val fontSheet = TextureRegionPack(ModMgr.getGdxFile("basegame", "fonts/7seg_small.tga"), W, H)

    init {
        setOwnsTexture(true)
    }

    override fun draw(batch: Batch, str: CharSequence, x: Float, y: Float): GlyphLayout? {

        str.forEachIndexed { index, c ->
            if (c in charMapping) {
                batch.draw(
                        fontSheet.get((c - ' ') % 16, (c - ' ') / 16),
                        x + W * index, y
                )
            }
        }


        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()
}