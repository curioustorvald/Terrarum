package com.Torvald;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by minjaesong on 16-02-15.
 */
public class JsonFetcher {

    private static StringBuffer jsonString;

    public static JsonObject readJson(String jsonFilePath) throws IOException {
        jsonString = new StringBuffer();
        readJsonFileAsString(jsonFilePath);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(jsonString.toString()).getAsJsonObject();

        return jsonObj;
    }

    private static void readJsonFileAsString(String path) throws IOException {
        Files.lines(
                FileSystems.getDefault().getPath(path)
        ).forEach(jsonString::append); // JSON does not require line break
    }
}
