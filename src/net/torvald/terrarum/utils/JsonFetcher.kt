package net.torvald.terrarum.utils

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.App.printdbg

/**
 * Created by minjaesong on 2016-02-15.
 */
object JsonFetcher {

    private var jsonString: StringBuffer? = null

    @Throws(java.nio.file.NoSuchFileException::class)
    operator fun invoke(jsonFilePath: String): JsonValue {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFilePath)

        printdbg(this, "Reading JSON $jsonFilePath")

        if (jsonString == null) {
            throw Error("[JsonFetcher] jsonString is null!")
        }

        return JsonReader().parse(jsonString.toString())
    }

    @Throws(java.nio.file.NoSuchFileException::class)
    operator fun invoke(jsonFile: java.io.File): JsonValue {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFile.canonicalPath)

        printdbg(this, "Reading JSON ${jsonFile.path}")

        if (jsonString == null) {
            throw Error("[JsonFetcher] jsonString is null!")
        }

        return JsonReader().parse(jsonString.toString())
    }

    @Throws(java.nio.file.NoSuchFileException::class)
    private fun readJsonFileAsString(path: String) {
        java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach(
                { jsonString!!.append(it) }
        ) // JSON does not require line break

    }

    fun forEach(map: JsonValue, action: (String, JsonValue) -> Unit) {
        var counter = 0
        var entry = map.child
        while (entry != null) {
            action(entry.name ?: "(arrayindex $counter)", entry)
            entry = entry.next
            counter += 1
        }
    }
}
