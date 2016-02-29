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
    static SpriteSheet uniHan;
    static SpriteSheet cyrilic;
    static SpriteSheet cyrilicEF;
    static SpriteSheet fullwidthForms;
    static SpriteSheet uniPunct;

    static final int JUNG_COUNT = 21;
    static final int JONG_COUNT = 28;

    static final int W_CJK = 10;
    static final int W_UNIHAN = 16;
    static final int W_LATIN_WIDE = 9; // width of regular letters, including m
    static final int W_LATIN_NARROW = 5; // width of letter f, t, i, l
    static final int H = 20;
    static final int H_HANGUL = 16;
    static final int H_UNIHAN = 16;
    static final int H_KANA = 20;

    static final int SHEET_ASCII_EM = 0;
    static final int SHEET_ASCII_EF = 1;
    static final int SHEET_HANGUL = 2;
    static final int SHEET_RUNIC = 3;
    static final int SHEET_EXTA_EM = 4;
    static final int SHEET_EXTA_EF = 5;
    static final int SHEET_KANA = 6;
    static final int SHEET_CJK_PUNCT = 7;
    static final int SHEET_UNIHAN = 8;
    static final int SHEET_CYRILIC_EM = 9;
    static final int SHEET_CYRILIC_EF = 10;
    static final int SHEET_FW_UNI = 11;
    static final int SHEET_UNI_PUNCT = 12;

    static SpriteSheet[] sheetKey;
    static final Character[] asciiEFList = {
             ' ','!','"','\'','(',')',',','.',':',';','I','[',']','`','f','i'
            ,'j','l','t','{','|','}',0xA1,'Ì','Í','Î','Ï','ì','í','î','ï','·'
    };

    static final Character[] extAEFList = {
            0x12E, 0x12F, 0x130, 0x131, 0x135, 0x13A, 0x13C, 0x142, 0x163, 0x167, 0x17F
    };

    static final Character[] cyrilecEFList = {
            0x406, 0x407, 0x456, 0x457, 0x458
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

    static int interchar = 0;


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

    private boolean isUniHan(char c) {
        return (c >= 0x3400 && c < 0xA000);
    }

    private boolean isCyrilic(char c) {
        return (c >= 0x400 && c < 0x460);
    }

    private boolean isCyrilicEF(char c) {
        return (Arrays.asList(cyrilecEFList).contains(c));
    }

    private boolean isFullwidthUni(char c) {
        return (c >= 0xFF00 && c < 0xFF20);
    }

    private boolean isUniPunct(char c) {
        return (c >= 0x2000 && c < 0x2070);
    }

    /** */

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

    private int uniHanIndexX(char c) {
        return (c - 0x3400) % 256;
    }

    private int uniHanIndexY(char c) {
        return (c - 0x3400) / 256;
    }

    private int cyrilicIndexX(char c) {
        return (c - 0x400) % 16;
    }

    private int cyrilicIndexY(char c) {
        return (c - 0x400) / 16;
    }

    private int cyrilicEFindexX(char c) {
        return (Arrays.asList(cyrilecEFList).indexOf(c) % 16);
    }

    private int cyrilicEFindexY(char c) {
        return (Arrays.asList(cyrilecEFList).indexOf(c) / 16);
    }

    private int fullwidthUniIndexX(char c) {
        return (c - 0xFF00) % 16;
    }

    private int fullwidthUniIndexY(char c) {
        return (c - 0xFF00) / 16;
    }

    private int uniPunctIndexX(char c) {
        return (c - 0x2000) % 16;
    }

    private int uniPunctIndexY(char c) {
        return (c - 0x2000) / 16;
    }

    @Override
    public int getWidth(String s) {
        return getWidthSubstr(s, s.length());
    }

    private int getWidthSubstr(String s, int endIndex) {
        int len = 0;
        for (int i = 0; i < endIndex; i++) {
            int c = getSheetType(s.charAt(i));

            if (i > 0 && s.charAt(i) > 0x20) { // Kerning
                int cpre = getSheetType(s.charAt(i - 1));
                if (
                        ((cpre == SHEET_UNIHAN || cpre == SHEET_HANGUL)
                                && !(c == SHEET_UNIHAN || c == SHEET_HANGUL))

                        || ((c == SHEET_UNIHAN || c == SHEET_HANGUL)
                                && !(cpre == SHEET_UNIHAN || cpre == SHEET_HANGUL))
                        ) {
                    // margin after/before hangul/unihan
                    len += 2;
                }
                else if ((c == SHEET_HANGUL || c == SHEET_KANA)
                        && (cpre == SHEET_HANGUL || cpre == SHEET_KANA)) {
                    // margin between hangul/kana
                    len += 1;
                }

            }

            if (c == SHEET_ASCII_EF || c == SHEET_EXTA_EF || c == SHEET_CYRILIC_EF)
                len += W_LATIN_NARROW;
            else if (c == SHEET_KANA || c == SHEET_HANGUL || c == SHEET_CJK_PUNCT)
                len += W_CJK;
            else if (c == SHEET_UNIHAN || c == SHEET_FW_UNI)
                len += W_UNIHAN;
            else
                len += W_LATIN_WIDE;

            if (i < endIndex - 1) len += interchar;
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
    public void drawString(float x, float y, String s) {
        // hangul fonts first
        hangulSheet.startUse();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (isHangul(ch)) {
                int[] hanPos = getHan(ch - 0xAC00);
                int glyphW = getWidth("" + ch);
                hangulSheet.renderInUse(
                        Math.round(x
                                + getWidthSubstr(s, i + 1) - glyphW
                        )
                        , Math.round((H - H_HANGUL) / 2 + y + 1)
                        , hanPos[0]
                        , hanPos[1]
                );
            }
        }
        hangulSheet.endUse();

        // unihan fonts
        uniHan.startUse();
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

        uniHan.endUse();

        //ascii fonts
        int prevInstance = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (!isHangul(ch) && !isUniHan(ch)) {

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
                    case SHEET_CYRILIC_EM:
                        sheetX = cyrilicIndexX(ch);
                        sheetY = cyrilicIndexY(ch);
                        break;
                    case SHEET_CYRILIC_EF:
                        sheetX = cyrilicEFindexX(ch);
                        sheetY = cyrilicEFindexY(ch);
                        break;
                    case SHEET_FW_UNI:
                        sheetX = fullwidthUniIndexX(ch);
                        sheetY = fullwidthUniIndexY(ch);
                        break;
                    case SHEET_UNI_PUNCT:
                        sheetX = uniPunctIndexX(ch);
                        sheetY = uniPunctIndexY(ch);
                        break;
                    default:
                        sheetX = ch % 16;
                        sheetY = ch / 16;
                        break;
                }

                try {
                    int glyphW = getWidth("" + ch);
                    sheetKey[prevInstance].renderInUse(
                            Math.round(x + getWidthSubstr(s, i + 1) - glyphW)
                                    // Interchar: pull punct right next to hangul to the left
                                    + ((i > 0 && isHangul(s.charAt(i - 1))) ? -3 : 0)
                            , Math.round(y)
                                    + ((prevInstance == SHEET_CJK_PUNCT) ? -1 :
                                       (prevInstance == SHEET_FW_UNI) ? (H - H_HANGUL) / 2 : 0)
                            , sheetX
                            , sheetY
                    );
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    // System.out.println("ArrayIndexOutOfBoundsException")
                    // System.out.println("char: '" + ch + "' (" + String.valueOf((int) ch) + ")");
                    // System.out.println("sheet: " + prevInstance);
                    // System.out.println("sheetX: " + sheetX);
                    // System.out.println("sheetY: " + sheetY);
                }

            }

        }
        if (prevInstance != -1) {
            sheetKey[prevInstance].endUse();
        }
    }

    @Override
    public void drawString(float x, float y, String s, Color color) {
        drawString(x, y, s);
    }

    private int getSheetType(char c) {
        // EFs
        if (isAsciiEF(c)) return SHEET_ASCII_EF;
        else if (isExtAEF(c)) return SHEET_EXTA_EF;
        else if (isCyrilicEF(c)) return SHEET_CYRILIC_EF;
        // fixed width
        else if (isRunic(c)) return SHEET_RUNIC;
        else if (isHangul(c)) return SHEET_HANGUL;
        else if (isKana(c)) return SHEET_KANA;
        else if (isUniHan(c)) return SHEET_UNIHAN;
        else if (isAscii(c)) return SHEET_ASCII_EM;
        else if (isExtA(c)) return SHEET_EXTA_EM;
        else if (isCyrilic(c)) return SHEET_CYRILIC_EM;
        else if (isUniPunct(c)) return SHEET_UNI_PUNCT;
        // fixed width punctuations
        else if (isCJKPunct(c)) return SHEET_CJK_PUNCT;
        else if (isFullwidthUni(c)) return SHEET_FW_UNI;

        else return SHEET_ASCII_EM; // fallback
    }

    /**
     * Draw part of a string to the screen. Note that this will still position the text as though
     * it's part of the bigger string.
     * @param x
     * @param y
     * @param s
     * @param color
     * @param startIndex
     * @param endIndex
     */
    @Override
    public void drawString(float x, float y, String s, Color color, int startIndex, int endIndex) {
        String unprintedHead = s.substring(0, startIndex);
        String printedBody = s.substring(startIndex, endIndex);
        int xoff = getWidth(unprintedHead);
        drawString(x + xoff, y, printedBody, color);
    }

    public void reloadUnihan() throws SlickException {

    }

    /**
     * Set margin between characters
     * @param margin
     */
    public void setInterchar(int margin) {
        interchar = margin;
    }
}
