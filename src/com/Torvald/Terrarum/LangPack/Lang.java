package com.Torvald.Terrarum.LangPack;

import com.Torvald.CSVFetcher;
import com.Torvald.ImageFont.GameFontWhite;
import com.Torvald.Terrarum.Terrarum;
import org.apache.commons.csv.CSVRecord;
import org.newdawn.slick.SlickException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * Created by minjaesong on 16-01-22.
 */
public class Lang {

    private static final String CSV_COLUMN_FIRST = "STRING_ID";
    /**
     * Get record by its STRING_ID
     */
    private static Hashtable<String, CSVRecord> lang;
    private static final String FALLBACK_LANG_CODE = "enUS";

    private static final int HANGUL_SYL_START = 0xAC00;

    private static final String PATH_TO_CSV = "./res/locales/";
    private static final String CSV_MAIN = "polyglot.csv";
    private static final String NAMESET_PREFIX = "nameset_";

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
        lang = new Hashtable<>();

        List<CSVRecord> langPackCSV = CSVFetcher.readCSV(PATH_TO_CSV + CSV_MAIN);

        File file = new File(PATH_TO_CSV);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(".csv") && !name.contains(CSV_MAIN) && !name.contains(NAMESET_PREFIX);
            }
        };
        for (String csvfilename : file.list(filter)) {
            List<CSVRecord> csv = CSVFetcher.readCSV(PATH_TO_CSV + csvfilename);
            csv.forEach(langPackCSV::add);
        }

        // Fill lang table
        langPackCSV.forEach(this::appendToLangByStringID);


        Arrays.sort(ENGLISH_WORD_NORMAL_PLURAL);
        Arrays.sort(FRENCH_WORD_NORMAL_PLURAL);

        try { ((GameFontWhite)Terrarum.gameFontWhite).reloadUnihan(); }
        catch (SlickException e) {}
    }

    private void appendToLangByStringID(CSVRecord record) {
        lang.put(record.get(CSV_COLUMN_FIRST), record);
    }

    public static CSVRecord getRecord(String key) {
        CSVRecord record = lang.get(key);
        if (record == null) {
            System.out.println("[Lang] No such record.");
            throw new NullPointerException();
        }
        return record;
    }

    public static String get(String key) {
        String value = null;
        try { value = lang.get(key).get(Terrarum.gameLocale); }
        catch (IllegalArgumentException e) { value = key; }
        return value;
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
