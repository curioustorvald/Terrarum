package com.Torvald;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by minjaesong on 16-02-15.
 */
public class JsonGetter {

    private static String jsonString = new String();

    public static JsonObject readJson(String jsonFileName) throws IOException {
        readJsonFileAsString(jsonFileName);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(jsonString).getAsJsonObject();

        return jsonObj;
    }

    private static void readJsonFileAsString(String path) throws IOException {
        Files.lines(
                FileSystems.getDefault().getPath(path)
        ).forEach(JsonGetter::strAppend);
    }

    private static void strAppend( String s) {
        jsonString += s;
    }

}
