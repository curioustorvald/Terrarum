package com.Torvald.ImageFont;

import com.Torvald.Terrarum.MapDrawer.LightmapRenderer;
import com.Torvald.Terrarum.Terrarum;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.*;

import java.util.Arrays;

/**
 * Created by minjaesong on 16-01-20.
 */
public class GameFontWhite extends GameFontBase {

    public GameFontWhite() throws SlickException {
        super();

        hangulSheet = new SpriteSheet(
                "./res/graphics/fonts/han_atlas.png"
                , W_CJK
                , H_CJK
        );
        asciiSheet = new SpriteSheet(
                "./res/graphics/fonts/ascii_majuscule.png"
                , W_EM
                , H
        );
        asciiSheetEF = new SpriteSheet(
                "./res/graphics/fonts/ascii_special_ef.png"
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
