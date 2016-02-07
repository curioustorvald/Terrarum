package com.Torvald.ImageFont;

import org.newdawn.slick.*;

/**
 * Created by minjaesong on 16-01-20.
 */
public class GameFontWhite extends GameFontBase {

    public GameFontWhite() throws SlickException {
        super();

        hangulSheet = new SpriteSheet(
                "./res/graphics/fonts/han_atlas.png"
                , W_CJK, H_CJK
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

        SpriteSheet[] shk = {
                asciiSheet
                , asciiSheetEF
                , hangulSheet
                , runicSheet
                , extASheet
                , extASheetEF
        };
        sheetKey = shk;
    }

}
