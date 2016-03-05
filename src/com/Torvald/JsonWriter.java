package com.Torvald;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by minjaesong on 16-03-04.
 */
public class JsonWriter {

    public static void writeFile(Object c, String path) throws IOException {
        JsonElement classElem = new Gson().toJsonTree(c);
        String jsonString = classElem.toString();
        FileWriter writer = new FileWriter(path);
        writer.write(jsonString);
        writer.close();
    }

}
