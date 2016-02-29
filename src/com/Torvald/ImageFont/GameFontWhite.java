package com.Torvald.ImageFont;

import com.Torvald.Terrarum.Terrarum;
import org.newdawn.slick.*;

/**
 * Created by minjaesong on 16-01-20.
 */
public class GameFontWhite extends GameFontBase {

    public GameFontWhite() throws SlickException {
        super();

        hangulSheet = new SpriteSheet(
                "./res/graphics/fonts/han_atlas.png"
                , W_CJK, H_HANGUL
        );
        asciiSheet = new SpriteSheet(
                "./res/graphics/fonts/ascii_majuscule.png"
                , W_LATIN_WIDE, H
        );
        asciiSheetEF = new SpriteSheet(
                "./res/graphics/fonts/ascii_special_ef.png"
                , W_LATIN_NARROW, H
        );
        runicSheet = new SpriteSheet(
                "./res/graphics/fonts/futhark.png"
                , W_LATIN_WIDE, H
        );
        extASheet = new SpriteSheet(
                "./res/graphics/fonts/LatinExtA_majuscule.png"
                , W_LATIN_WIDE, H
        );
        extASheetEF = new SpriteSheet(
                "./res/graphics/fonts/LatinExtA_ef.png"
                , W_LATIN_NARROW, H
        );
        kanaSheet = new SpriteSheet(
                "./res/graphics/fonts/kana.png"
                , W_CJK, H_KANA
        );
        cjkPunct = new SpriteSheet(
                "./res/graphics/fonts/cjkpunct.png"
                , W_CJK, H_KANA
        );
        uniHan = new SpriteSheet(
                "./res/graphics/fonts/unifont_unihan"
                        + ((!Terrarum.gameLocale.contains("zh"))
                        ? "_ja" : "")
                        +".png"
                , W_UNIHAN, H_UNIHAN
        );
        cyrilic = new SpriteSheet(
                "./res/graphics/fonts/cyrilic_majuscule.png"
                , W_LATIN_WIDE, H
        );
        cyrilicEF = new SpriteSheet(
                "./res/graphics/fonts/cyrilic_ef.png"
                , W_LATIN_NARROW, H
        );
        fullwidthForms = new SpriteSheet(
                "./res/graphics/fonts/fullwidth_forms.png"
                , W_UNIHAN, H_UNIHAN
        );
        uniPunct = new SpriteSheet(
                "./res/graphics/fonts/unipunct.png"
                , W_LATIN_WIDE, H
        );

        SpriteSheet[] shk = {
                  asciiSheet
                , asciiSheetEF
                , hangulSheet
                , runicSheet
                , extASheet
                , extASheetEF
                , kanaSheet
                , cjkPunct
                , uniHan
                , cyrilic
                , cyrilicEF
                , fullwidthForms
                , uniPunct
        };
        sheetKey = shk;
    }

    @Override
    public void reloadUnihan() throws SlickException {
        uniHan = new SpriteSheet(
                "./res/graphics/fonts/unifont_unihan"
                        + ((!Terrarum.gameLocale.contains("zh"))
                           ? "_ja" : "")
                        +".png"
                , W_UNIHAN, H_UNIHAN
        );
    }
}
