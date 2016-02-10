package com.Torvald.ImageFont;

import org.newdawn.slick.*;

import java.util.Arrays;

/**
 * Created by minjaesong on 16-01-27.
 */
public class GameFontBase implements Font {

    static SpriteSheet hangulSheet;
    static SpriteSheet asciiSheet;
    static SpriteSheet asciiSheetEF;
    static SpriteSheet runicSheet;
    static SpriteSheet extASheet;
    static SpriteSheet extASheetEF;
    static SpriteSheet kanaSheet;
    static SpriteSheet cjkPunct;

    static final int JUNG_COUNT = 21;
    static final int JONG_COUNT = 28;

    static final int W_CJK = 10;
    static final int W_CJK_DRAW = 11;
    static final int W_LATIN_WIDE = 9; // width of regular letters, including m
    static final int W_LATIN_NARROW = 5; // width of letter f, t, i, l
    static final int H = 20;
    static final int H_HANGUL = 16;
    static final int H_KANA = 20;

    static final int SHEET_ASCII_EM = 0;
    static final int SHEET_ASCII_EF = 1;
    static final int SHEET_HANGUL = 2;
    static final int SHEET_RUNIC = 3;
    static final int SHEET_EXTA_EM = 4;
    static final int SHEET_EXTA_EF = 5;
    static final int SHEET_KANA = 6;
    static final int SHEET_CJK_PUNCT = 7;

    static SpriteSheet[] sheetKey;
    static final Character[] asciiEFList = {
             ' ','!','"','\'','(',')','I','[',']','`','f','i','j','l','t','{'
            ,'|','}',0xA1,'Ì','Í','Î','Ï','ì','í','î','ï','·'
    };

    static final Character[] extAEFList = {
            0x12E, 0x12F, 0x130, 0x131, 0x135, 0x13A, 0x13C, 0x142, 0x163, 0x167, 0x17F
    };

    /**
     * Runic letters list used for game. The set is
     * Younger Futhark + Medieval rune 'e' + Punct + Runic Almanac
     *
     * BEWARE OF SIMILAR-LOOKING RUNES, especially:
     *
     * * Algiz ᛉ instead of Maðr ᛘ
     *
     * * Short-Twig Hagall ᚽ instead of Runic Letter E ᛂ
     *
     * * Runic Letter OE ᚯ instead of Óss ᚬ
     *
     * Examples:
     * ᛭ᛋᛁᚴᚱᛁᚦᛦ᛭
     * ᛭ᛂᛚᛋᛅ᛭ᛏᚱᚢᛏᚾᛁᚾᚴᚢᚾᛅ᛬ᛅᚱᚾᛅᛏᛅᛚᛋ
     */
    static final Character[] runicList = {
             'ᚠ','ᚢ','ᚦ','ᚬ','ᚱ','ᚴ','ᚼ','ᚾ','ᛁ','ᛅ','ᛋ','ᛏ','ᛒ','ᛘ','ᛚ','ᛦ'
            ,'ᛂ','᛬','᛫','᛭','ᛮ','ᛯ','ᛰ'
    };


    public GameFontBase() throws SlickException {

    }

    private int[] getHan(int hanIndex) {
        int han_x = hanIndex % JONG_COUNT;
        int han_y = hanIndex / JONG_COUNT;
        int[] ret = {han_x, han_y};
        return ret;
    }

    private boolean isAsciiEF(char c) {
        return (Arrays.asList(asciiEFList).contains(c));
    }

    private boolean isExtAEF(char c) {
        return (Arrays.asList(extAEFList).contains(c));
    }

    private boolean isHangul(char c) {
        return (c >= 0xAC00 && c < 0xD7A4);
    }

    private boolean isAscii(char c) { return (c > 0 && c <= 0xFF); }

    private boolean isRunic(char c) {
        return (Arrays.asList(runicList).contains(c));
    }

    private boolean isExtA(char c) {
        return (c >= 0x100 && c < 0x180);
    }

    private boolean isKana(char c) {
        return (c >= 0x3040 && c < 0x3100);
    }

    private boolean isCJKPunct(char c) {
        return (c >= 0x3000 && c < 0x3040);
    }

    private int asciiEFindexX(char c) {
        return (Arrays.asList(asciiEFList).indexOf(c) % 16);
    }

    private int asciiEFindexY(char c) {
        return (Arrays.asList(asciiEFList).indexOf(c) / 16);
    }

    private int extAEFindexX(char c) {
        return (Arrays.asList(extAEFList).indexOf(c) % 16);
    }

    private int extAEFindexY(char c) {
        return (Arrays.asList(extAEFList).indexOf(c) / 16);
    }

    private int runicIndexX(char c) {
        return (Arrays.asList(runicList).indexOf(c) % 16);
    }

    private int runicIndexY(char c) {
        return (Arrays.asList(runicList).indexOf(c) / 16);
    }

    private int kanaIndexX(char c) {
        return (c - 0x3040) % 16;
    }

    private int kanaIndexY(char c) {
        return (c - 0x3040) / 16;
    }

    private int cjkPunctIndexX(char c) {
        return (c - 0x3000) % 16;
    }

    private int cjkPunctIndexY(char c) {
        return (c - 0x3000) / 16;
    }

    @Override
    public int getWidth(String s) {
        int len = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (getSheetType(c)) {
                case SHEET_ASCII_EF:
                    len += W_LATIN_NARROW; break;
                case SHEET_HANGUL: case SHEET_KANA: case SHEET_CJK_PUNCT:
                    len += W_CJK_DRAW; break;
                default:
                    len += W_LATIN_WIDE;
            }
        }
        return len;
    }

    @Override
    public int getHeight(String s) {
        return H;
    }

    @Override
    public int getLineHeight() {
        return H;
    }

    @Override
    public void drawString(float v, float v1, String s) {

    }

    @Override
    public void drawString(float x, float y, String s, Color color) {
        // hangul fonts first
        hangulSheet.startUse();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (isHangul(ch)) {
                int[] hanPos = getHan(ch - 0xAC00);
                hangulSheet.renderInUse(
                        Math.round(x
                                + getWidth(s.substring(0, i))
                        )
                        , Math.round((H - H_HANGUL) / 2 + y + 1)
                        , hanPos[0]
                        , hanPos[1]
                );
            }
        }
        hangulSheet.endUse();

        //ascii fonts
        int prevInstance = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (!isHangul(ch)) {

                // if not init, enduse first
                if (prevInstance != -1) {
                    sheetKey[prevInstance].endUse();
                }
                sheetKey[getSheetType(ch)].startUse();
                prevInstance = getSheetType(ch);

                int sheetX;
                int sheetY;
                switch (prevInstance) {
                    case SHEET_ASCII_EF:
                        sheetX = asciiEFindexX(ch);
                        sheetY = asciiEFindexY(ch);
                        break;
                    case SHEET_EXTA_EF:
                        sheetX = extAEFindexX(ch);
                        sheetY = extAEFindexY(ch);
                        break;
                    case SHEET_RUNIC:
                        sheetX = runicIndexX(ch);
                        sheetY = runicIndexY(ch);
                        break;
                    case SHEET_EXTA_EM:
                        sheetX = (ch - 0x100) % 16;
                        sheetY = (ch - 0x100) / 16;
                        break;
                    case SHEET_KANA:
                        sheetX = kanaIndexX(ch);
                        sheetY = kanaIndexY(ch);
                        break;
                    case SHEET_CJK_PUNCT:
                        sheetX = cjkPunctIndexX(ch);
                        sheetY = cjkPunctIndexY(ch);
                        break;
                    default:
                        sheetX = ch % 16;
                        sheetY = ch / 16;
                        break;
                }

                try {
                    sheetKey[prevInstance].renderInUse(
                            Math.round(x
                                    + getWidth(s.substring(0, i))
                            )
                            , Math.round(y)
                            , sheetX
                            , sheetY
                    );
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("char: '" + ch + "' (" + String.valueOf((int) ch) + ")");
                    System.out.println("sheet: " + prevInstance);
                    System.out.println("sheetX: " + sheetX);
                    System.out.println("sheetY: " + sheetY);
                    e.printStackTrace();
                }

            }

        }
        if (prevInstance != -1) {
            sheetKey[prevInstance].endUse();
        }
    }

    private int getSheetType(char c) {
        if (isAsciiEF(c)) return SHEET_ASCII_EF;
        else if (isHangul(c)) return SHEET_HANGUL;
        else if (isRunic(c)) return SHEET_RUNIC;
        else if (isExtA(c)) return SHEET_EXTA_EM;
        else if (isExtAEF(c)) return SHEET_EXTA_EF;
        else if (isKana(c)) return SHEET_KANA;
        else if (isCJKPunct(c)) return SHEET_CJK_PUNCT;
        else return SHEET_ASCII_EM;
    }

    @Override
    public void drawString(float v, float v1, String s, Color color, int i, int i1) {

    }

}
