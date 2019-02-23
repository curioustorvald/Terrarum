package net.torvald.terrarum.utils

import com.google.gson.GsonBuilder
import net.torvald.terrarum.AppLoader

/**
 * Created by minjaesong on 2016-03-04.
 */
object JsonWriter {

    private val formattingRegex = Regex("""(?<=[\{,\[])|(?=[\]}])""")

    fun getJsonBuilder() = if (AppLoader.IS_DEVELOPMENT_BUILD) {
        getPrettyBuilder()
    }
    else {
        GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .enableComplexMapKeySerialization()
                .create()
    }

    fun getPrettyBuilder() = GsonBuilder()
            .setPrettyPrinting()

            .serializeNulls()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()

    /**
     * serialise a class to the file as JSON, using Google GSON.
     *
     * @param c: a class
     * @param path: path to write a file
     */
    @Throws(java.io.IOException::class)
    fun writeToFile(c: Any, path: String) {
        val jsonString = getJsonBuilder().toJson(c)

        val writer = java.io.FileWriter(path, false)
        writer.write(jsonString.replace(formattingRegex, "\n"))
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
        val writer = java.io.FileWriter(path, false)

        writer.write(getPrettyBuilder().toJson(jsonObject))
        //writer.write(jsonObject.toString().replace(formattingRegex, "\n"))
        writer.close()
    }

}
