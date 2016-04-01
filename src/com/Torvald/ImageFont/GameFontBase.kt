package com.torvald.imagefont

import com.torvald.terrarum.Terrarum
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*

/**
 * Created by minjaesong on 16-01-27.
 */
open class GameFontBase @Throws(SlickException::class)
constructor() : Font {

    private fun getHan(hanIndex: Int): IntArray {
        val han_x = hanIndex % JONG_COUNT
        val han_y = hanIndex / JONG_COUNT
        val ret = intArrayOf(han_x, han_y)
        return ret
    }

    private fun getHanChosung(hanIndex: Int) = hanIndex / (JUNG_COUNT * JONG_COUNT)

    private fun getHanJungseong(hanIndex: Int) = hanIndex / JONG_COUNT % JUNG_COUNT

    private fun getHanJongseong(hanIndex: Int) = hanIndex % JONG_COUNT


    private fun getHanChoseongRow(hanIndex: Int): Int {
        val jungseongIndex = getHanJungseong(hanIndex)
        val jungseongWide = arrayOf(8, 12, 13, 17, 18, 21)
        val jungseongComplex = arrayOf(9, 10, 11, 14, 15, 16, 22)
        val ret: Int

        if (jungseongWide.contains(jungseongIndex)) {
            ret = 2
        }
        else if (jungseongComplex.contains(jungseongIndex)) {
            ret = 4
        }
        else {
            ret = 0
        }
        return if (getHanJongseong(hanIndex) == 0) ret else ret + 1
    }

    private fun getHanJungseongRow(hanIndex: Int) = if (getHanJongseong(hanIndex) == 0) 6 else 7

    private val hanJongseongRow: Int
        get() = 8

    private fun isAsciiEF(c: Char) = asciiEFList.contains(c)

    private fun isExtAEF(c: Char) = extAEFList.contains(c)

    private fun isHangul(c: Char) = c.toInt() >= 0xAC00 && c.toInt() < 0xD7A4

    private fun isAscii(c: Char) = c.toInt() > 0 && c.toInt() <= 0xFF

    private fun isRunic(c: Char) = runicList.contains(c)

    private fun isExtA(c: Char) = c.toInt() >= 0x100 && c.toInt() < 0x180

    private fun isKana(c: Char) = c.toInt() >= 0x3040 && c.toInt() < 0x3100

    private fun isCJKPunct(c: Char) = c.toInt() >= 0x3000 && c.toInt() < 0x3040

    private fun isUniHan(c: Char) = c.toInt() >= 0x3400 && c.toInt() < 0xA000

    private fun isCyrilic(c: Char) = c.toInt() >= 0x400 && c.toInt() < 0x460

    private fun isCyrilicEF(c: Char) = cyrilecEFList.contains(c)

    private fun isFullwidthUni(c: Char) = c.toInt() >= 0xFF00 && c.toInt() < 0xFF20

    private fun isUniPunct(c: Char) = c.toInt() >= 0x2000 && c.toInt() < 0x2070

    private fun isWenQuanYi1(c: Char) = c.toInt() >= 0x33F3 && c.toInt() <= 0x69FC

    private fun isWenQuanYi2(c: Char) = c.toInt() >= 0x69FD && c.toInt() <= 0x9FDC



    private fun asciiEFindexX(c: Char) = asciiEFList.indexOf(c) % 16
    private fun asciiEFindexY(c: Char) = asciiEFList.indexOf(c) / 16

    private fun extAEFindexX(c: Char) = extAEFList.indexOf(c) % 16
    private fun extAEFindexY(c: Char) = extAEFList.indexOf(c) / 16

    private fun runicIndexX(c: Char) = runicList.indexOf(c) % 16
    private fun runicIndexY(c: Char) = runicList.indexOf(c) / 16

    private fun kanaIndexX(c: Char) = (c.toInt() - 0x3040) % 16
    private fun kanaIndexY(c: Char) = (c.toInt() - 0x3040) / 16

    private fun cjkPunctIndexX(c: Char) = (c.toInt() - 0x3000) % 16
    private fun cjkPunctIndexY(c: Char) = (c.toInt() - 0x3000) / 16

    private fun uniHanIndexX(c: Char) = (c.toInt() - 0x3400) % 256
    private fun uniHanIndexY(c: Char) = (c.toInt() - 0x3400) / 256

    private fun cyrilicIndexX(c: Char) = (c.toInt() - 0x400) % 16
    private fun cyrilicIndexY(c: Char) = (c.toInt() - 0x400) / 16

    private fun cyrilicEFindexX(c: Char) = cyrilecEFList.indexOf(c) % 16
    private fun cyrilicEFindexY(c: Char) = cyrilecEFList.indexOf(c) / 16

    private fun fullwidthUniIndexX(c: Char) = (c.toInt() - 0xFF00) % 16
    private fun fullwidthUniIndexY(c: Char) = (c.toInt() - 0xFF00) / 16

    private fun uniPunctIndexX(c: Char) = (c.toInt() - 0x2000) % 16
    private fun uniPunctIndexY(c: Char) = (c.toInt() - 0x2000) / 16

    private fun wenQuanYiIndexX(c: Char) =
            (c.toInt() - if (c.toInt() <= 0x4DB5) 0x33F3 else 0x33F3 + 0x4A) % 32
    private fun wenQuanYi1IndexY(c: Char) = (c.toInt() - (0x33F3 + 0x4A)) / 32
    private fun wenQuanYi2IndexY(c: Char) = (c.toInt() - 0x69FD) / 32

    override fun getWidth(s: String) = getWidthSubstr(s, s.length)


    private fun getWidthSubstr(s: String, endIndex: Int): Int {
        var len = 0
        for (i in 0..endIndex - 1) {
            val c = getSheetType(s[i])

            if (i > 0 && s[i].toInt() > 0x20) {
                // Kerning
                val cpre = getSheetType(s[i - 1])
                if ((cpre == SHEET_UNIHAN || cpre == SHEET_HANGUL) && !(c == SHEET_UNIHAN || c == SHEET_HANGUL)

                    || (c == SHEET_UNIHAN || c == SHEET_HANGUL) && !(cpre == SHEET_UNIHAN || cpre == SHEET_HANGUL)) {
                    // margin after/before hangul/unihan
                    len += 2
                }
                else if ((c == SHEET_HANGUL || c == SHEET_KANA) && (cpre == SHEET_HANGUL || cpre == SHEET_KANA)) {
                    // margin between hangul/kana
                    len += 1
                }

            }

            if (c == SHEET_ASCII_EF || c == SHEET_EXTA_EF || c == SHEET_CYRILIC_EF)
                len += W_LATIN_NARROW
            else if (c == SHEET_KANA || c == SHEET_HANGUL || c == SHEET_CJK_PUNCT)
                len += W_CJK
            else if (c == SHEET_UNIHAN || c == SHEET_FW_UNI || c == SHEET_WENQUANYI_1 || c == SHEET_WENQUANYI_2)
                len += W_UNIHAN
            else
                len += W_LATIN_WIDE

            if (i < endIndex - 1) len += interchar
        }
        return len
    }

    override fun getHeight(s: String) = H

    override fun getLineHeight() = H

    override fun drawString(x: Float, y: Float, s: String) {
        drawString(x, y, s, Color.white)
    }

    override fun drawString(x: Float, y: Float, s: String, color: Color) {
        // hangul fonts first
        hangulSheet.startUse()
        // JOHAB
        for (i in 0..s.length - 1) {
            val ch = s[i]

            if (isHangul(ch)) {
                val hIndex = ch.toInt() - 0xAC00

                val indexCho = getHanChosung(hIndex)
                val indexJung = getHanJungseong(hIndex)
                val indexJong = getHanJongseong(hIndex)

                val choRow = getHanChoseongRow(hIndex)
                val jungRow = getHanJungseongRow(hIndex)
                val jongRow = hanJongseongRow

                val glyphW = getWidth("" + ch)

                // chosung
                hangulSheet.renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW), Math.round(((H - H_HANGUL) / 2).toFloat() + y + 1f), indexCho, choRow)
                // jungseong
                hangulSheet.renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW), Math.round(((H - H_HANGUL) / 2).toFloat() + y + 1f), indexJung, jungRow)
                // jongseong
                hangulSheet.renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW), Math.round(((H - H_HANGUL) / 2).toFloat() + y + 1f), indexJong, jongRow)
            }
        }
        hangulSheet.endUse()

        // unihan fonts
        /*uniHan.startUse();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (isUniHan(ch)) {
                int glyphW = getWidth("" + ch);
                uniHan.renderInUse(
                        Math.round(x
                                + getWidthSubstr(s, i + 1) - glyphW
                        )
                        , Math.round((H - H_UNIHAN) / 2 + y)
                        , uniHanIndexX(ch)
                        , uniHanIndexY(ch)
                );
            }
        }

        uniHan.endUse();*/

        // WenQuanYi 1
        wenQuanYi_1.startUse()

        for (i in 0..s.length - 1) {
            val ch = s[i]

            if (isWenQuanYi1(ch)) {
                val glyphW = getWidth("" + ch)
                wenQuanYi_1.renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW),
                        Math.round((H - H_UNIHAN) / 2 + y),
                        wenQuanYiIndexX(ch),
                        wenQuanYi1IndexY(ch)
                )
            }
        }

        wenQuanYi_1.endUse()
        wenQuanYi_2.startUse()

        for (i in 0..s.length - 1) {
            val ch = s[i]

            if (isWenQuanYi2(ch)) {
                val glyphW = getWidth("" + ch)
                wenQuanYi_2.renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW),
                        Math.round((H - H_UNIHAN) / 2 + y),
                        wenQuanYiIndexX(ch),
                        wenQuanYi2IndexY(ch)
                )
            }
        }

        wenQuanYi_2.endUse()

        //ascii fonts
        var prevInstance = -1
        for (i in 0..s.length - 1) {
            val ch = s[i]

            if (!isHangul(ch) && !isUniHan(ch)) {

                // if not init, endUse first
                if (prevInstance != -1) {
                    sheetKey[prevInstance].endUse()
                }
                sheetKey[getSheetType(ch)].startUse()
                prevInstance = getSheetType(ch)

                val sheetX: Int
                val sheetY: Int
                when (prevInstance) {
                    SHEET_ASCII_EF   -> {
                        sheetX = asciiEFindexX(ch)
                        sheetY = asciiEFindexY(ch)
                    }
                    SHEET_EXTA_EF    -> {
                        sheetX = extAEFindexX(ch)
                        sheetY = extAEFindexY(ch)
                    }
                    SHEET_RUNIC      -> {
                        sheetX = runicIndexX(ch)
                        sheetY = runicIndexY(ch)
                    }
                    SHEET_EXTA_EM    -> {
                        sheetX = (ch.toInt() - 0x100) % 16
                        sheetY = (ch.toInt() - 0x100) / 16
                    }
                    SHEET_KANA       -> {
                        sheetX = kanaIndexX(ch)
                        sheetY = kanaIndexY(ch)
                    }
                    SHEET_CJK_PUNCT  -> {
                        sheetX = cjkPunctIndexX(ch)
                        sheetY = cjkPunctIndexY(ch)
                    }
                    SHEET_CYRILIC_EM -> {
                        sheetX = cyrilicIndexX(ch)
                        sheetY = cyrilicIndexY(ch)
                    }
                    SHEET_CYRILIC_EF -> {
                        sheetX = cyrilicEFindexX(ch)
                        sheetY = cyrilicEFindexY(ch)
                    }
                    SHEET_FW_UNI     -> {
                        sheetX = fullwidthUniIndexX(ch)
                        sheetY = fullwidthUniIndexY(ch)
                    }
                    SHEET_UNI_PUNCT  -> {
                        sheetX = uniPunctIndexX(ch)
                        sheetY = uniPunctIndexY(ch)
                    }
                    else             -> {
                        sheetX = ch.toInt() % 16
                        sheetY = ch.toInt() / 16
                    }
                }

                val glyphW = getWidth("" + ch)
                sheetKey[prevInstance].renderInUse(
                        Math.round(x + getWidthSubstr(s, i + 1) - glyphW) // Interchar: pull punct right next to hangul to the left
                        + if (i > 0 && isHangul(s[i - 1])) -3 else 0, Math.round(y) +
                                                                      if (prevInstance == SHEET_CJK_PUNCT)
                                                                          -1
                                                                      else if (prevInstance == SHEET_FW_UNI)
                                                                          (H - H_HANGUL) / 2
                                                                      else 0,
                        sheetX, sheetY)

            }

        }
        if (prevInstance != -1) {
            sheetKey[prevInstance].endUse()
        }
    }

    private fun getSheetType(c: Char): Int {
        // EFs
        if (isAsciiEF(c))
            return SHEET_ASCII_EF
        else if (isExtAEF(c))
            return SHEET_EXTA_EF
        else if (isCyrilicEF(c))
            return SHEET_CYRILIC_EF
        else if (isRunic(c))
            return SHEET_RUNIC
        else if (isHangul(c))
            return SHEET_HANGUL
        else if (isKana(c))
            return SHEET_KANA
        else if (isUniHan(c))
            return SHEET_UNIHAN
        else if (isAscii(c))
            return SHEET_ASCII_EM
        else if (isExtA(c))
            return SHEET_EXTA_EM
        else if (isCyrilic(c))
            return SHEET_CYRILIC_EM
        else if (isUniPunct(c))
            return SHEET_UNI_PUNCT
        else if (isCJKPunct(c))
            return SHEET_CJK_PUNCT
        else if (isFullwidthUni(c))
            return SHEET_FW_UNI
        else
            return SHEET_ASCII_EM// fixed width punctuations
        // fixed width
        // fallback
    }

    /**
     * Draw part of a string to the screen. Note that this will still position the text as though
     * it's part of the bigger string.
     * @param x
     * *
     * @param y
     * *
     * @param s
     * *
     * @param color
     * *
     * @param startIndex
     * *
     * @param endIndex
     */
    override fun drawString(x: Float, y: Float, s: String, color: Color, startIndex: Int, endIndex: Int) {
        val unprintedHead = s.substring(0, startIndex)
        val printedBody = s.substring(startIndex, endIndex)
        val xoff = getWidth(unprintedHead)
        drawString(x + xoff, y, printedBody, color)
    }

    @Throws(SlickException::class)
    open fun reloadUnihan() {

    }

    /**
     * Set margin between characters
     * @param margin
     */
    fun setInterchar(margin: Int) {
        interchar = margin
    }

    private fun setBlendModeMul() {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun setBlendModeNormal() {
        GL11.glDisable(GL11.GL_BLEND)
        Terrarum.appgc.graphics.setDrawMode(Graphics.MODE_NORMAL)
    }

    companion object {

        lateinit internal var hangulSheet: SpriteSheet
        lateinit internal var asciiSheet: SpriteSheet
        lateinit internal var asciiSheetEF: SpriteSheet
        lateinit internal var runicSheet: SpriteSheet
        lateinit internal var extASheet: SpriteSheet
        lateinit internal var extASheetEF: SpriteSheet
        lateinit internal var kanaSheet: SpriteSheet
        lateinit internal var cjkPunct: SpriteSheet
        // static SpriteSheet uniHan;
        lateinit internal var cyrilic: SpriteSheet
        lateinit internal var cyrilicEF: SpriteSheet
        lateinit internal var fullwidthForms: SpriteSheet
        lateinit internal var uniPunct: SpriteSheet
        lateinit internal var wenQuanYi_1: SpriteSheet
        lateinit internal var wenQuanYi_2: SpriteSheet

        internal val JUNG_COUNT = 21
        internal val JONG_COUNT = 28

        internal val W_CJK = 10
        internal val W_UNIHAN = 16
        internal val W_LATIN_WIDE = 9 // width of regular letters, including m
        internal val W_LATIN_NARROW = 5 // width of letter f, t, i, l
        internal val H = 20
        internal val H_HANGUL = 16
        internal val H_UNIHAN = 16
        internal val H_KANA = 20

        internal val SHEET_ASCII_EM = 0
        internal val SHEET_ASCII_EF = 1
        internal val SHEET_HANGUL = 2
        internal val SHEET_RUNIC = 3
        internal val SHEET_EXTA_EM = 4
        internal val SHEET_EXTA_EF = 5
        internal val SHEET_KANA = 6
        internal val SHEET_CJK_PUNCT = 7
        internal val SHEET_UNIHAN = 8
        internal val SHEET_CYRILIC_EM = 9
        internal val SHEET_CYRILIC_EF = 10
        internal val SHEET_FW_UNI = 11
        internal val SHEET_UNI_PUNCT = 12
        internal val SHEET_WENQUANYI_1 = 13
        internal val SHEET_WENQUANYI_2 = 14

        lateinit internal var sheetKey: Array<SpriteSheet>
        internal val asciiEFList = arrayOf(' ', '!', '"', '\'', '(', ')', ',', '.', ':', ';', 'I', '[', ']', '`', 'f', 'i', 'j', 'l', 't', '{', '|', '}', 0xA1.toChar(), 'Ì', 'Í', 'Î', 'Ï', 'ì', 'í', 'î', 'ï', '·')

        internal val extAEFList = arrayOf(
                0x12E.toChar(),
                0x12F.toChar(),
                0x130.toChar(),
                0x131.toChar(),
                0x135.toChar(),
                0x13A.toChar(),
                0x13C.toChar(),
                0x142.toChar(),
                0x163.toChar(),
                0x167.toChar(),
                0x17F.toChar()
        )

        internal val cyrilecEFList = arrayOf(
                0x406.toChar(),
                0x407.toChar(),
                0x456.toChar(),
                0x457.toChar(),
                0x458.toChar()
        )

        /**
         * Runic letters list used for game. The set is
         * Younger Futhark + Medieval rune 'e' + Punct + Runic Almanac

         * BEWARE OF SIMILAR-LOOKING RUNES, especially:

         * * Algiz ᛉ instead of Maðr ᛘ

         * * Short-Twig Hagall ᚽ instead of Runic Letter E ᛂ

         * * Runic Letter OE ᚯ instead of Óss ᚬ

         * Examples:
         * ᛭ᛋᛁᚴᚱᛁᚦᛦ᛭
         * ᛭ᛂᛚᛋᛅ᛭ᛏᚱᚢᛏᚾᛁᚾᚴᚢᚾᛅ᛬ᛅᚱᚾᛅᛏᛅᛚᛋ
         */
        internal val runicList = arrayOf('ᚠ', 'ᚢ', 'ᚦ', 'ᚬ', 'ᚱ', 'ᚴ', 'ᚼ', 'ᚾ', 'ᛁ', 'ᛅ', 'ᛋ', 'ᛏ', 'ᛒ', 'ᛘ', 'ᛚ', 'ᛦ', 'ᛂ', '᛬', '᛫', '᛭', 'ᛮ', 'ᛯ', 'ᛰ')

        internal var interchar = 0
    }
}
