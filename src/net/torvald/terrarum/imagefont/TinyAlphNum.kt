package net.torvald.terrarum.imagefont

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 16-04-15.
 */
object TinyAlphNum : BitmapFont() {

    internal val W = 8
    internal val H = 8

    internal val fontSheet = TextureRegionPack("./assets/graphics/fonts/milky.tga", W, H)


    init {
        setOwnsTexture(true)
        setUseIntegerPositions(true)
    }

    fun getWidth(str: String): Int {
        return W * str.length
    }

    override fun draw(batch: Batch, text: CharSequence, x: Float, y: Float): GlyphLayout? {
        text.forEachIndexed { index, c ->
            batch.draw(fontSheet.get(index % 16, index / 16), x + index * W, y)
        }

        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()
}