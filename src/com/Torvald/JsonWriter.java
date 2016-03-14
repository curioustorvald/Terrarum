package com.Torvald;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by minjaesong on 16-03-04.
 */
public class JsonWriter {

    public static void writeToFile(Object c, String path) throws IOException {
        JsonElement classElem = new Gson().toJsonTree(c);
        String jsonString = classElem.toString();
        FileWriter writer = new FileWriter(path);
        writer.write(jsonString);
        writer.close();
    }

    public static void writeToFile(JsonObject jsonObject, String path) throws IOException {
        FileWriter writer = new FileWriter(path);
        writer.write(jsonObject.toString());
        writer.close();
    }

}
