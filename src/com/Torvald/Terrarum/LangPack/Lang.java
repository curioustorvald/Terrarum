package com.Torvald.Terrarum.LangPack;

import com.Torvald.Terrarum.Terrarum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Created by minjaesong on 16-01-22.
 */
public class Lang {

    private static Properties lang;
    private static Properties langFallback;
    private static final String FALLBACK_LANG_CODE = "en";

    public Lang() throws IOException {
        lang = new Properties();
        lang.load(new BufferedReader(new InputStreamReader(new FileInputStream(
                "res/locales/" + Terrarum.gameLocale + ".lang"), StandardCharsets.UTF_8)));

        langFallback = new Properties();
        langFallback.load(new BufferedReader(new InputStreamReader(new FileInputStream(
                "res/locales/" + FALLBACK_LANG_CODE + ".lang"), StandardCharsets.UTF_8)));
    }

    public static String get(String key) {
        return lang.getProperty(key
                ,langFallback.getProperty(key
                        , key)
        );
    }

}
