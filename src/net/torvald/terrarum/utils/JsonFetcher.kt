package net.torvald.terrarum.utils

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

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFilePath: String): com.google.gson.JsonObject {
        net.torvald.terrarum.utils.JsonFetcher.jsonString = StringBuffer() // reset buffer every time it called
        net.torvald.terrarum.utils.JsonFetcher.readJsonFileAsString(jsonFilePath)

        println("[JsonFetcher] Reading JSON $jsonFilePath")

        val jsonParser = com.google.gson.JsonParser()
        val jsonObj = jsonParser.parse(net.torvald.terrarum.utils.JsonFetcher.jsonString!!.toString()).asJsonObject

        return jsonObj
    }

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFile: java.io.File): com.google.gson.JsonObject {
        net.torvald.terrarum.utils.JsonFetcher.jsonString = StringBuffer() // reset buffer every time it called
        net.torvald.terrarum.utils.JsonFetcher.readJsonFileAsString(jsonFile.canonicalPath)

        println("[JsonFetcher] Reading JSON ${jsonFile.path}")

        val jsonParser = com.google.gson.JsonParser()
        val jsonObj = jsonParser.parse(net.torvald.terrarum.utils.JsonFetcher.jsonString!!.toString()).asJsonObject

        return jsonObj
    }

    private fun readJsonFileAsString(path: String) {
        try {
            java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach(
                    { net.torvald.terrarum.utils.JsonFetcher.jsonString!!.append(it) }
            ) // JSON does not require line break
        }
        catch (e: IOException) {
            System.err.println("An error occurred while reading $path")
            e.printStackTrace()
        }
    }
}
