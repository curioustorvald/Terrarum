package net.torvald.terrarum.imagefont

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import net.torvald.terrarum.roundToFloat
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2016-04-15.
 */
object TinyAlphNum : BitmapFont() {

    internal const val W = 7
    internal const val H = 13

    internal val fontSheet = TextureRegionPack("./assets/graphics/fonts/7x13_Tamzen7x14b.tga", W+1, H+1)
    internal val fontPixmap = Pixmap(Gdx.files.internal("./assets/graphics/fonts/7x13_Tamzen7x14b.tga"))

    init {
        setOwnsTexture(true)
        setUseIntegerPositions(true)
    }

    override fun dispose() {
        fontSheet.dispose()
        fontPixmap.dispose()
    }

    fun getWidth(str: String): Int {
        var l = 0
        for (char in str) {
            if (!isColourCodeHigh(char) && !isColourCodeLow(char)) {
                l += 1
            }
        }
        return W * l
    }

    private var colMain = Color.WHITE
    private var colMainInt = -1

    override fun draw(batch: Batch, text: CharSequence, x: Float, y: Float): GlyphLayout? {
        val originalColour = batch.color.cpy()
        colMain = batch.color.cpy()

        val x = x.roundToFloat()
        val y = y.roundToFloat()

        var charsPrinted = 0
        text.forEachIndexed { index, c ->
            if (isColourCodeHigh(c)) {
                val cchigh = c
                val cclow = text[index + 1]
                val colour = getColour(cchigh, cclow)

                colMain = colour
            }
            else if (c in 0.toChar()..255.toChar()) {
                batch.color = colMain
                batch.draw(fontSheet.get(c.code % 16, c.code / 16), x + charsPrinted * W, y)

                charsPrinted += 1
            }
        }


        batch.color = originalColour

        return null
    }

    fun drawToPixmap(pixmap: Pixmap, text: String, x: Int, y: Int) {
        var charsPrinted = 0


        text.forEachIndexed { index, c ->
            if (isColourCodeHigh(c)) {
                val cchigh = c
                val cclow = text[index + 1]
                val colour = getColour(cchigh, cclow)

                colMainInt = colour.toRGBA8888()
            }
            else if (c in 0.toChar()..255.toChar()) {
                val srcX = (c.code % 16) * (W+1)
                val srcY = (c.code / 16) * (H+1)
                val destX = x + charsPrinted * W
                val destY = y

                pixmap.drawPixmap(fontPixmap, srcX, srcY, W+1, H+1, destX, destY, colMainInt)

                charsPrinted += 1
            }
        }
    }


    /***
     * @param col RGBA8888 representation
     */
    private fun Pixmap.drawPixmap(pixmap: Pixmap, srcX: Int, srcY: Int, srcW: Int, srcH: Int, destX: Int, destY: Int, col: Int) {
        for (y in srcY until srcY + srcH) {
            for (x in srcX until srcX + srcW) {
                val pixel = pixmap.getPixel(x, y)

                val newPixel = pixel colorTimes col

                this.drawPixel(destX + x - srcX, destY + y - srcY, newPixel)
            }
        }
    }

    private fun Color.toRGBA8888() =
        (this.r * 255f).toInt().shl(24) or
                (this.g * 255f).toInt().shl(16) or
                (this.b * 255f).toInt().shl(8) or
                (this.a * 255f).toInt()

    /**
     * RGBA8888 representation
     */
    private fun Int.forceOpaque() = this.and(0xFFFFFF00.toInt()) or 0xFF

    private infix fun Int.colorTimes(other: Int): Int {
        val thisBytes = IntArray(4) { this.ushr(it * 8).and(255) }
        val otherBytes = IntArray(4) { other.ushr(it * 8).and(255) }

        return (thisBytes[0] times256 otherBytes[0]) or
                (thisBytes[1] times256 otherBytes[1]).shl(8) or
                (thisBytes[2] times256 otherBytes[2]).shl(16) or
                (thisBytes[3] times256 otherBytes[3]).shl(24)
    }

    private infix fun Int.times256(other: Int) = multTable255[this][other]

    private val multTable255 = Array(256) { left ->
        IntArray(256) { right ->
            (255f * (left / 255f).times(right / 255f)).roundToInt()
        }
    }




    override fun getLineHeight() = H.toFloat()
    override fun getCapHeight() = getLineHeight()
    override fun getXHeight() = getLineHeight()



    private fun isColourCodeHigh(c: Char) = c.code in 0b110110_1111000000..0b110110_1111111111
    private fun isColourCodeLow(c: Char) = c.code in 0b110111_0000000000..0b110111_1111111111

    private fun getColour(charHigh: Char, charLow: Char): Color { // input: 0x10ARGB, out: RGBA8888
        val codePoint = Character.toCodePoint(charHigh, charLow)

        if (colourBuffer.containsKey(codePoint))
            return colourBuffer[codePoint]!!

        val a = codePoint.and(0xF000).ushr(12)
        val r = codePoint.and(0x0F00).ushr(8)
        val g = codePoint.and(0x00F0).ushr(4)
        val b = codePoint.and(0x000F)

        val col = Color(r.shl(28) or r.shl(24) or g.shl(20) or g.shl(16) or b.shl(12) or b.shl(8) or a.shl(4) or a)


        colourBuffer[codePoint] = col
        return col
    }

    private val colourBuffer = HashMap<Int, Color>()
}