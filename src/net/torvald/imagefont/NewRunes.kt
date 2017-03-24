package net.torvald.imagefont

import org.newdawn.slick.Color
import org.newdawn.slick.Font
import org.newdawn.slick.SpriteSheet

/**
 * Created by SKYHi14 on 2017-03-24.
 */
class NewRunes : Font {
    private val runeSize = 12

    private val encPlane = IntArray(128, {
        if (it < 0x10)
            0x20 + it
        else if (it < 0x30)
            0x3000 + (it - 0x20)
        else
            0x3130 + (it - 0x30)
    })

    private fun codeToEnc(c: Char): Int? {
        val result =  encPlane.binarySearch(c.toInt())
        return if (result >= 0) result else null
    }

    private val runes = SpriteSheet("./assets/graphics/fonts/newrunes.tga", runeSize, runeSize)

    var scale = 1
    var linegap = 8


    override fun getHeight(str: String) = 12

    override fun drawString(x: Float, y: Float, text: String) = drawString(x, y, text, Color.white)

    override fun drawString(x: Float, y: Float, text: String, col: Color) {
        text.forEachIndexed { index, c ->
            val encodePoint = codeToEnc(c)

            if (encodePoint != null) {
                runes.getSubImage(encodePoint % 16, encodePoint / 16).draw(
                        x + runeSize * index * scale, y, scale.toFloat(), col
                )
            }
        }
    }

    override fun drawString(x: Float, y: Float, text: String, col: Color, startIndex: Int, endIndex: Int) {
        UnsupportedOperationException("not implemented")
    }

    override fun getWidth(str: String) = runeSize * str.length

    override fun getLineHeight() = (runeSize + linegap) * scale
}