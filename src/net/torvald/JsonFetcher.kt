package net.torvald

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.ArrayList
import java.util.function.Consumer

/**
 * Created by minjaesong on 16-02-15.
 */
object JsonFetcher {

    private var jsonString: StringBuffer? = null

    @Throws(IOException::class)
    fun readJson(jsonFilePath: String): JsonObject {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFilePath)

        println("Reading JSON $jsonFilePath")

        val jsonParser = JsonParser()
        val jsonObj = jsonParser.parse(jsonString!!.toString()).asJsonObject

        return jsonObj
    }

    @Throws(IOException::class)
    fun readJson(jsonFile: File): JsonObject {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFile.canonicalPath)

        println("Reading JSON ${jsonFile.path}")

        val jsonParser = JsonParser()
        val jsonObj = jsonParser.parse(jsonString!!.toString()).asJsonObject

        return jsonObj
    }

    @Throws(IOException::class)
    private fun readJsonFileAsString(path: String) {
        Files.lines(FileSystems.getDefault().getPath(path)).forEach(
                { jsonString!!.append(it) }
        ) // JSON does not require line break
    }
}
