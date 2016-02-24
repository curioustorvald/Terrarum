package com.Torvald;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by minjaesong on 16-02-15.
 */
public class JsonFetcher {

    private static StringBuffer jsonString;

    public static JsonObject readJson(String jsonFilePath) throws IOException {
        jsonString = new StringBuffer(); // reset buffer every time it called
        readJsonFileAsString(jsonFilePath);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(jsonString.toString()).getAsJsonObject();

        return jsonObj;
    }

    public static ArrayList<String> readJsonAsString(String jsonFilePath) throws IOException {
        ArrayList<String> jsonFileLines = new ArrayList<>();
        Files.lines(
                FileSystems.getDefault().getPath(jsonFilePath)
        ).forEach(jsonFileLines::add);
        return jsonFileLines;
    }

    private static void readJsonFileAsString(String path) throws IOException {
        Files.lines(
                FileSystems.getDefault().getPath(path)
        ).forEach(jsonString::append); // JSON does not require line break
    }
}
