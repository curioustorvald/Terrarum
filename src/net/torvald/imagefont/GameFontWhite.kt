package net.torvald.imagefont

import org.newdawn.slick.*

/**
 * Created by minjaesong on 16-01-20.
 */
class GameFontWhite : GameFontBase() {

    init {

        GameFontBase.hangulSheet = SpriteSheet(
                "./assets/graphics/fonts/hangul_johab.png", GameFontBase.W_HANGUL, GameFontBase.H_HANGUL)
        GameFontBase.asciiSheet = SpriteSheet(
                "./assets/graphics/fonts/ascii_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.asciiSheetEF = SpriteSheet(
                "./assets/graphics/fonts/ascii_special_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.runicSheet = SpriteSheet(
                "./assets/graphics/fonts/futhark.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.extASheet = SpriteSheet(
                "./assets/graphics/fonts/LatinExtA_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.extASheetEF = SpriteSheet(
                "./assets/graphics/fonts/LatinExtA_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.kanaSheet = SpriteSheet(
                "./assets/graphics/fonts/kana.png", GameFontBase.W_KANA, GameFontBase.H_KANA)
        GameFontBase.cjkPunct = SpriteSheet(
                "./assets/graphics/fonts/cjkpunct.png", GameFontBase.W_ASIAN_PUNCT, GameFontBase.H_KANA)
        /*uniHan = new SpriteSheet(
                "./assets/graphics/fonts/unifont_unihan"
                        + ((!terrarum.gameLocale.contains("zh"))
                        ? "_ja" : "")
                        +".png"
                , W_UNIHAN, H_UNIHAN
        );*/
        GameFontBase.cyrilic = SpriteSheet(
                "./assets/graphics/fonts/cyrilic_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.cyrilicEF = SpriteSheet(
                "./assets/graphics/fonts/cyrilic_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.fullwidthForms = SpriteSheet(
                "./assets/graphics/fonts/fullwidth_forms.png", GameFontBase.W_UNIHAN, GameFontBase.H_UNIHAN)
        GameFontBase.uniPunct = SpriteSheet(
                "./assets/graphics/fonts/unipunct.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.wenQuanYi_1 = SpriteSheet(
                "./assets/graphics/fonts/wenquanyi_11pt_part1.png", 16, 18, 2)
        GameFontBase.wenQuanYi_2 = SpriteSheet(
                "./assets/graphics/fonts/wenquanyi_11pt_part2.png", 16, 18, 2)
        GameFontBase.greekSheet = SpriteSheet(
                "./assets/graphics/fonts/greek_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.greekSheetEF = SpriteSheet(
                "./assets/graphics/fonts/greek_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.romanianSheet = SpriteSheet(
                "./assets/graphics/fonts/romana_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.romanianSheetEF = SpriteSheet(
                "./assets/graphics/fonts/romana_ef.png", GameFontBase.W_LATIN_NARROW, GameFontBase.H)
        GameFontBase.thaiSheet = SpriteSheet(
                "./assets/graphics/fonts/thai_fullwidth.png", GameFontBase.W_LATIN_WIDE, GameFontBase.H)
        GameFontBase.keycapSheet = SpriteSheet(
                "./assets/graphics/fonts/puae000-e07f.png", GameFontBase.SIZE_KEYCAP, GameFontBase.SIZE_KEYCAP)

        val shk = arrayOf(
                GameFontBase.asciiSheet,
                GameFontBase.asciiSheetEF,
                GameFontBase.hangulSheet,
                GameFontBase.runicSheet,
                GameFontBase.extASheet,
                GameFontBase.extASheetEF,
                GameFontBase.kanaSheet,
                GameFontBase.cjkPunct,
                null, // Full unihan, filler because we're using WenQuanYi
                GameFontBase.cyrilic,
                GameFontBase.cyrilicEF,
                GameFontBase.fullwidthForms,
                GameFontBase.uniPunct,
                GameFontBase.wenQuanYi_1,
                GameFontBase.wenQuanYi_2,
                GameFontBase.greekSheet,
                GameFontBase.greekSheetEF,
                GameFontBase.romanianSheet,
                GameFontBase.romanianSheetEF,
                GameFontBase.thaiSheet,
                null, // Thai EF, filler because not being used right now
                GameFontBase.keycapSheet
        )
        GameFontBase.sheetKey = shk
    }
}
