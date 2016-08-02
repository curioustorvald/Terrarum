package net.torvald.imagefont

import org.newdawn.slick.Color
import org.newdawn.slick.Font
import org.newdawn.slick.SpriteSheet
import java.util.*

/**
 * Created by minjaesong on 16-04-15.
 */
class TinyAlphNum : Font {

    internal val fontSheet: SpriteSheet

    internal val W = 8
    internal val H = 8

    private val chars = arrayOf(
            '0','1','2','3','4','5','6','7',
            '8','9','[','#','@',':','>','?',
            ' ','A','B','C','D','E','F','G',
            'H','I','&','.',']','(','<','\\',
            '^','J','K','L','M','N','O','P',
            'Q','R','-','¤','*',')',';','\'',
            '+','/','S','T','U','V','W','X',
            'Y','Z','_',',','%','=','"','!'
    )
    private val mappingTable = HashMap<Int, Int>()

    init {
        fontSheet = SpriteSheet("./assets/graphics/fonts/alphanumeric_small.png", W, H)
        chars.forEachIndexed { i, c -> mappingTable[c.toInt()] = i }
    }

    override fun getHeight(str: String): Int = H

    override fun getWidth(str: String): Int {
        var ret = 0
        for (i in 0..str.length - 1) {
            val c = str.codePointAt(i).toChar()
            if (!c.isColourCode())
                ret += W
        }
        return ret
    }

    override fun getLineHeight(): Int = H

    override fun drawString(x: Float, y: Float, text: String) = drawString(x, y, text, Color.white)

    override fun drawString(x: Float, y: Float, text: String, col: Color) {
        var thisCol = col
        var textPosOffset = 0
        for (i in 0..text.length - 1) {
            val index = charToSpriteNum(text.toUpperCase().codePointAt(i))
            val ch = text[i]

            if (ch.isColourCode()) {
                thisCol = GameFontBase.colourKey[ch]!!
                continue
            }
            if (index != null) {
                fontSheet.getSubImage(index % 8, index / 8).draw(
                        x + textPosOffset, y, thisCol
                )
            }
            textPosOffset += W
        }
    }

    override fun drawString(x: Float, y: Float, text: String, col: Color, startIndex: Int, endIndex: Int) {
        throw UnsupportedOperationException()
    }

    private fun charToSpriteNum(ch: Int): Int? = mappingTable[ch]

    fun Char.isColourCode() = GameFontBase.colourKey.containsKey(this)
}