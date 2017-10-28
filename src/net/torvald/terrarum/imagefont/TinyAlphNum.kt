package net.torvald.terrarum.imagefont

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.round
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2016-04-15.
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

    lateinit var colourHolder: Color

    override fun draw(batch: Batch, text: CharSequence, x: Float, y: Float): GlyphLayout? {
        val originalColour = batch.color
        colourHolder = batch.color

        val x = x.round()
        val y = y.round()

        var charsPrinted = 0
        text.forEachIndexed { index, c ->
            if (isColourCodeHigh(c)) {
                val cchigh = c
                val cclow = text[index + 1]
                val colour = getColour(cchigh, cclow)

                colourHolder = colour
            }
            else if (c in 0.toChar()..255.toChar()) {
                batch.color = colourHolder.cpy().mul(0.5f, 0.5f, 0.5f, 1f)
                batch.draw(fontSheet.get(c.toInt() % 16, c.toInt() / 16), x + charsPrinted * W + 1, y)
                batch.draw(fontSheet.get(c.toInt() % 16, c.toInt() / 16), x + charsPrinted * W    , y + 1)
                batch.draw(fontSheet.get(c.toInt() % 16, c.toInt() / 16), x + charsPrinted * W + 1, y + 1)


                batch.color = colourHolder.cpy()
                batch.draw(fontSheet.get(c.toInt() % 16, c.toInt() / 16), x + charsPrinted * W, y)

                charsPrinted += 1
            }
        }


        batch.color = originalColour

        return null
    }

    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()



    private fun isColourCodeHigh(c: Char) = c.toInt() in 0b110110_1111000000..0b110110_1111111111
    private fun isColourCodeLow(c: Char) = c.toInt() in 0b110111_0000000000..0b110111_1111111111

    private fun getColour(charHigh: Char, charLow: Char): Color { // input: 0x10ARGB, out: RGBA8888
        val codePoint = Character.toCodePoint(charHigh, charLow)

        if (colourBuffer.containsKey(codePoint))
            return colourBuffer[codePoint]!!

        val r = codePoint.and(0xF000).ushr(12)
        val g = codePoint.and(0x0F00).ushr(8)
        val b = codePoint.and(0x00F0).ushr(4)
        val a = codePoint.and(0x000F)

        val col = Color(r.shl(28) or r.shl(24) or g.shl(20) or g.shl(16) or b.shl(12) or b.shl(8) or a.shl(4) or a)


        colourBuffer[codePoint] = col
        return col
    }

    private val colourBuffer = HashMap<Int, Color>()
}