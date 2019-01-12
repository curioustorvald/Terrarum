package net.torvald.terrarum.utils

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
        val classElem = com.google.gson.Gson().toJsonTree(c)
        val jsonString = classElem.toString()
        val writer = java.io.FileWriter(path)
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
        val writer = java.io.FileWriter(path)
        writer.write(jsonObject.toString().replace(formattingRegex, "\n"))
        writer.close()
    }

}
