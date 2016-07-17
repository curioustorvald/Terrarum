package net.torvald.imagefont

import net.torvald.terrarum.Terrarum
import org.newdawn.slick.*

/**
 * Created by minjaesong on 16-01-20.
 */
class GameFontWhite @Throws(SlickException::class)
constructor() : GameFontBase() {

    init {

        GameFontBase.hangulSheet = SpriteSheet(
                "./res/graphics/fonts/han_johab.png", GameFontBase.W_CJK, GameFontBase.H_HANGUL)
        GameFontBase.asciiSheet = SpriteSheet(
                "./res/graphics/fonts/ascii_majuscule.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.asciiSheetEF = SpriteSheet(
                "./res/graphics/fonts/ascii_special_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.runicSheet = SpriteSheet(
                "./res/graphics/fonts/futhark.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.extASheet = SpriteSheet(
                "./res/graphics/fonts/LatinExtA_majuscule.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.extASheetEF = SpriteSheet(
                "./res/graphics/fonts/LatinExtA_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.kanaSheet = SpriteSheet(
                "./res/graphics/fonts/kana.png", GameFontBase.W_CJK, GameFontBase.H_KANA)
        GameFontBase.cjkPunct = SpriteSheet(
                "./res/graphics/fonts/cjkpunct.png", GameFontBase.W_CJK, GameFontBase.H_KANA)
        /*uniHan = new SpriteSheet(
                "./res/graphics/fonts/unifont_unihan"
                        + ((!terrarum.gameLocale.contains("zh"))
                        ? "_ja" : "")
                        +".png"
                , W_UNIHAN, H_UNIHAN
        );*/
        GameFontBase.cyrilic = SpriteSheet(
                "./res/graphics/fonts/cyrilic_majuscule.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.cyrilicEF = SpriteSheet(
                "./res/graphics/fonts/cyrilic_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.fullwidthForms = SpriteSheet(
                "./res/graphics/fonts/fullwidth_forms.png", GameFontBase.W_UNIHAN, GameFontBase.H_UNIHAN)
        GameFontBase.uniPunct = SpriteSheet(
                "./res/graphics/fonts/unipunct.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.wenQuanYi_1 = SpriteSheet(
                "./res/graphics/fonts/wenquanyi_11pt_part1.png", 16, 18, 2)
        GameFontBase.wenQuanYi_2 = SpriteSheet(
                "./res/graphics/fonts/wenquanyi_11pt_part2.png", 16, 18, 2)
        GameFontBase.greekSheet = SpriteSheet(
                "./res/graphics/fonts/greek_majuscule.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.greekSheetEF = SpriteSheet(
                "./res/graphics/fonts/greek_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.romanianSheet = SpriteSheet(
                "./res/graphics/fonts/romana_majuscule.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.romanianSheetEF = SpriteSheet(
                "./res/graphics/fonts/romana_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.thaiSheet = SpriteSheet(
                "./res/graphics/fonts/thai_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)

        val shk = arrayOf(
                GameFontBase.asciiSheet,
                GameFontBase.asciiSheetEF,
                GameFontBase.hangulSheet,
                GameFontBase.runicSheet,
                GameFontBase.extASheet,
                GameFontBase.extASheetEF,
                GameFontBase.kanaSheet,
                GameFontBase.cjkPunct,
                null, // Filler
                GameFontBase.cyrilic,
                GameFontBase.cyrilicEF,
                GameFontBase.fullwidthForms,
                GameFontBase.uniPunct,
                GameFontBase.wenQuanYi_1,
                GameFontBase.wenQuanYi_2, // uniHan
                GameFontBase.greekSheet,
                GameFontBase.greekSheetEF,
                GameFontBase.romanianSheet,
                GameFontBase.romanianSheetEF,
                GameFontBase.thaiSheet,
                null // Filler
        )
        GameFontBase.sheetKey = shk
    }
}
