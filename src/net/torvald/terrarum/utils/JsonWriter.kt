package net.torvald.terrarum.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

import java.io.FileWriter
import java.io.IOException

/**
 * Created by minjaesong on 2016-03-04.
 */
object JsonWriter {

    /**
     * serialise a class to the file as JSON, using Google GSON.
     *
     * @param c: a class
     * @param path: path to write a file
     */
    @Throws(java.io.IOException::class)
    fun writeToFile(c: Any, path: String) {
        val classElem = com.google.gson.Gson().toJsonTree(c)
        val jsonString = classElem.toString()
        val writer = java.io.FileWriter(path)
        writer.write(jsonString)
        writer.close()
    }

    /**
     * serialise JsonObject to the file as JSON, using Google GSON.
     *
     * @param jsonObject
     * @param path: path to write a file
     */
    @Throws(java.io.IOException::class)
    fun writeToFile(jsonObject: com.google.gson.JsonObject, path: String) {
        val writer = java.io.FileWriter(path)
        writer.write(jsonObject.toString())
        writer.close()
    }

}
