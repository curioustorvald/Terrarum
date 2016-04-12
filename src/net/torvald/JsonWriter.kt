package net.torvald

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

import java.io.FileWriter
import java.io.IOException

/**
 * Created by minjaesong on 16-03-04.
 */
object JsonWriter {

    /**
     * serialise a class to the file as JSON, using Google GSON.
     *
     * @param c: a class
     * @param path: path to write a file
     */
    @Throws(IOException::class)
    fun writeToFile(c: Any, path: String) {
        val classElem = Gson().toJsonTree(c)
        val jsonString = classElem.toString()
        val writer = FileWriter(path)
        writer.write(jsonString)
        writer.close()
    }

    /**
     * serialise JsonObject to the file as JSON, using Google GSON.
     *
     * @param jsonObject
     * @param path: path to write a file
     */
    @Throws(IOException::class)
    fun writeToFile(jsonObject: JsonObject, path: String) {
        val writer = FileWriter(path)
        writer.write(jsonObject.toString())
        writer.close()
    }

}
