package com.Torvald.Terrarum.LangPack;

import com.Torvald.Terrarum.Terrarum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by minjaesong on 16-01-22.
 */
public class Lang {

    private static Properties lang;
    private static Properties langFallback;
    private static final String FALLBACK_LANG_CODE = "en";

    private static final int HANGUL_SYL_START = 0xAC00;

    private static final int[] HANGUL_POST_INDEX_ALPH = { // 0: 는, 가, ...  1: 은, 이, ...
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] HANGUL_POST_RO_INDEX_ALPH = { // 0: 로  1: 으로
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static String[] ENGLISH_WORD_NORMAL_PLURAL = {
            "photo"
    };

    private static String[] FRENCH_WORD_NORMAL_PLURAL = {
              "bal"
            , "banal"
            , "fatal"
            , "final"
    };

    public Lang() throws IOException {
        lang = new Properties();
        lang.load(new BufferedReader(new InputStreamReader(new FileInputStream(
                "res/locales/" + Terrarum.gameLocale + ".lang"), StandardCharsets.UTF_8)));

        langFallback = new Properties();
        langFallback.load(new BufferedReader(new InputStreamReader(new FileInputStream(
                "res/locales/" + FALLBACK_LANG_CODE + ".lang"), StandardCharsets.UTF_8)));

        Arrays.sort(ENGLISH_WORD_NORMAL_PLURAL);
    }

    public static String get(String key) {
        return lang.getProperty(key
                ,langFallback.getProperty(key
                        , key)
        );
    }

    public static String pluraliseLang(String key, int count) {
        return (count > 1) ? get(key + "_PLURAL") : get(key);
    }

    public static String pluralise(String word, int count) {
        if (count < 2) return word;

        switch (Terrarum.gameLocale) {
            case ("fr"):
                if (Arrays.binarySearch(FRENCH_WORD_NORMAL_PLURAL, word) >= 0) {
                    return word + "s";
                }
                if (word.endsWith("al") || word.endsWith("au") || word.endsWith("eu") || word
                        .endsWith("eau")) {
                    return word.substring(0, word.length() - 2) + "ux";
                }
                else if (word.endsWith("ail")) {
                    return word.substring(0, word.length() - 3) + "ux";
                }
                else {
                    return word + "s";
                }
            case ("en"): default:
                if (Arrays.binarySearch(ENGLISH_WORD_NORMAL_PLURAL, word) >= 0) {
                    return word + "s";
                }
                else if (word.endsWith("f")) { // f -> ves
                    return word.substring(0, word.length() - 2) + "ves";
                }
                else if (word.endsWith("o") || word.endsWith("z")) { // o -> oes
                    return word + "es";
                }
                else {
                    return word + "s";
                }
        }
    }

    public static String postEunNeun(String word) {
        char lastChar = getLastChar(word);

        if (isHangul(lastChar)) {
            int index = lastChar - HANGUL_SYL_START;
            return (index % 28 == 0) ? word + "는" : word + "은";
        }
        else if ((lastChar >= 'A' && lastChar <= 'Z')
                || (lastChar >= 'a' && lastChar <= 'z')) {
            int index = (lastChar - 0x41) % 0x20;
            return (HANGUL_POST_INDEX_ALPH[index] == 0) ? word + "는" : word + "은";
        }
        else {
            return "은(는)";
        }
    }

    public static String postIiGa(String word) {
        char lastChar = getLastChar(word);

        if (isHangul(lastChar)) {
            int index = lastChar - HANGUL_SYL_START;
            return (index % 28 == 0) ? word + "가" : word + "이";
        }
        else if ((lastChar >= 'A' && lastChar <= 'Z')
                || (lastChar >= 'a' && lastChar <= 'z')) {
            int index = (lastChar - 0x41) % 0x20;
            return (HANGUL_POST_INDEX_ALPH[index] == 0) ? word + "가" : word + "이";
        }
        else {
            return "이(가)";
        }
    }

    private static boolean isHangul(char c) {
        return (c >= 0xAC00 && c <= 0xD7A3);
    }

    private static char getLastChar(String s) {
        return s.charAt(s.length() - 1);
    }
}
