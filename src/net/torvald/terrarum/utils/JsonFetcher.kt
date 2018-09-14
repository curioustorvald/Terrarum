package net.torvald.terrarum.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.torvald.terrarum.AppLoader.printdbg
import java.io.File

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.ArrayList
import java.util.function.Consumer

/**
 * Created by minjaesong on 2016-02-15.
 */
object JsonFetcher {

    private var jsonString: StringBuffer? = null

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFilePath: String): com.google.gson.JsonObject {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFilePath)

        printdbg(this, "Reading JSON $jsonFilePath")

        if (jsonString == null) {
            throw Error("[JsonFetcher] jsonString is null!")
        }

        val jsonParser = com.google.gson.JsonParser()
        val jsonObj = jsonParser.parse(jsonString.toString()).asJsonObject

        return jsonObj
    }

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFile: java.io.File): com.google.gson.JsonObject {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFile.canonicalPath)

        printdbg(this, "Reading JSON ${jsonFile.path}")

        if (jsonString == null) {
            throw Error("[JsonFetcher] jsonString is null!")
        }

        val jsonParser = com.google.gson.JsonParser()
        val jsonObj = jsonParser.parse(jsonString.toString()).asJsonObject

        return jsonObj
    }

    private fun readJsonFileAsString(path: String) {
        try {
            java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach(
                    { jsonString!!.append(it) }
            ) // JSON does not require line break
        }
        catch (e: IOException) {
            System.err.println("[JsonFetcher] An error occurred while reading $path")
            e.printStackTrace()
        }
    }
}
