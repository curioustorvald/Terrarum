package com.Torvald.ImageFont;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

/**
 * Created by minjaesong on 16-01-27.
 */
public class GameFontBlack extends GameFontBase {

    public GameFontBlack() throws SlickException {
        super();

        hangulSheet = new SpriteSheet(
                "./res/graphics/fonts/han_atlas_black.png"
                , W_CJK
                , H_CJK
        );
        asciiSheet = new SpriteSheet(
                "./res/graphics/fonts/ascii_majuscule_black.png"
                , W_EM
                , H
        );
        asciiSheetEF = new SpriteSheet(
                "./res/graphics/fonts/ascii_special_ef_black.png"
                , W_EF
                , H
        );

        SpriteSheet[] shk = {
                asciiSheet
                , asciiSheetEF
                , hangulSheet
        };
        sheetKey = shk;
    }
}
