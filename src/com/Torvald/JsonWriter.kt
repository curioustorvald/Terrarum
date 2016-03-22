package com.Torvald

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

import java.io.FileWriter
import java.io.IOException

/**
 * Created by minjaesong on 16-03-04.
 */
object JsonWriter {

    @Throws(IOException::class)
    fun writeToFile(c: Any, path: String) {
        val classElem = Gson().toJsonTree(c)
        val jsonString = classElem.toString()
        val writer = FileWriter(path)
        writer.write(jsonString)
        writer.close()
    }

    @Throws(IOException::class)
    fun writeToFile(jsonObject: JsonObject, path: String) {
        val writer = FileWriter(path)
        writer.write(jsonObject.toString())
        writer.close()
    }

}
