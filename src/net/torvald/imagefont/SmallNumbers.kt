package net.torvald.imagefont

import org.newdawn.slick.Color
import org.newdawn.slick.Font
import org.newdawn.slick.SpriteSheet

/**
 * Created by minjaesong on 16-04-15.
 */
class SmallNumbers : Font {

    internal val fontSheet: SpriteSheet

    internal val W = 8
    internal val H = 8

    private val chars = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-')

    init {
        fontSheet = SpriteSheet("./res/graphics/fonts/numeric_small.png", W, H)
    }

    override fun getHeight(str: String): Int = H

    override fun getWidth(str: String): Int = str.length * W

    override fun getLineHeight(): Int = H

    override fun drawString(x: Float, y: Float, text: String) = drawString(x, y, text, Color.white)

    override fun drawString(x: Float, y: Float, text: String, col: Color) {
        for (i in 0..text.length - 1) {
            val index = charToSpriteNum(text.codePointAt(i))
            if (index != null) {
                fontSheet.getSubImage(index, 0).draw(
                        x + i * W, y, col
                )
            }
        }
    }

    override fun drawString(x: Float, y: Float, text: String, col: Color, startIndex: Int, endIndex: Int) {
        throw UnsupportedOperationException()
    }

    private fun charToSpriteNum(ch: Int): Int? =
            if (ch in '0'.toInt()..'9'.toInt()) ch - '0'.toInt()
            else if (ch == '-'.toInt())         10
            else null
}