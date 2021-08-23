package net.torvald.terrarum.utils

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/**
 * Created by minjaesong on 2016-03-04.
 */
object JsonWriter {

    private val formattingRegex = Regex("""(?<=[\{,\[])|(?=[\]}])""")

    /**
     * serialise a class to the file as JSON, using Google GSON.
     *
     * @param c: a class
     * @param path: path to write a file
     */
    @Throws(java.io.IOException::class)
    fun writeToFile(c: Any, path: String) {
        val writer = java.io.FileWriter(path, false)
        writer.write(Json(JsonWriter.OutputType.json).toJson(c).replace(formattingRegex, "\n"))
        writer.close()
    }

    /**
     * serialise JsonObject to the file as JSON, using Google GSON.
     *
     * @param jsonObject
     * @param path: path to write a file
     */
    /*@Throws(java.io.IOException::class)
    fun writeToFile(jsonObject: Json, path: String) {
        val writer = java.io.FileWriter(path, false)

        writer.write(jsonObject.)
        //writer.write(jsonObject.toString().replace(formattingRegex, "\n"))
        writer.close()
    }*/

}
