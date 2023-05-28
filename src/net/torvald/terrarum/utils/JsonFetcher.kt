package net.torvald.terrarum.utils

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.App.printdbg
import java.io.Reader

/**
 * Created by minjaesong on 2016-02-15.
 */
object JsonFetcher {

    private var jsonString: StringBuffer? = null

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFilePath: String): JsonValue {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFilePath)

        printdbg(this, "Reading JSON $jsonFilePath")

        if (jsonString == null) throw Error("[JsonFetcher] jsonString is null!")

        return JsonReader().parse(jsonString.toString())
    }

    @Throws(java.io.IOException::class)
    operator fun invoke(jsonFile: java.io.File): JsonValue {
        jsonString = StringBuffer() // reset buffer every time it called
        readJsonFileAsString(jsonFile.canonicalPath)

        printdbg(this, "Reading JSON ${jsonFile.path}")

        if (jsonString == null) throw Error("[JsonFetcher] jsonString is null!")

        return JsonReader().parse(jsonString.toString())
    }

    fun readFromJsonString(stringReader: Reader): JsonValue {
        return JsonReader().parse(stringReader.readText())
    }

    @Throws(java.io.IOException::class)
    private fun readJsonFileAsString(path: String) {
        java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach(
                { jsonString!!.append(it) }
        ) // JSON does not require line break

    }

    /**
     * Iterates [JsonValue] over its siblings.
     *
     * @param map JsonValue to iterate over
     * @param action A `function(`Name of the sibling or a stringified integer if the `map` is an array`, `JsonValue representation of the sibling`)` -> `Unit`
     */
    fun forEachSiblings(map: JsonValue, action: (String, JsonValue) -> Unit) {
        var counter = 0
        var entry = map.child
        while (entry != null) {
            action(entry.name ?: "$counter", entry)
            entry = entry.next
            counter += 1
        }
    }

    /**
     * Iterates [JsonValue] over its siblings.
     *
     * @param map JsonValue to iterate over
     * @param action A `function(index, `Name of the sibling or a stringified integer if the `map` is an array`, `JsonValue representation of the sibling`)` -> `Unit`
     */
    fun forEachSiblingsIndexed(map: JsonValue, action: (Int, String, JsonValue) -> Unit) {
        var counter = 0
        var entry = map.child
        while (entry != null) {
            action(counter, entry.name ?: "$counter", entry)
            entry = entry.next
            counter += 1
        }
    }
}

/**
 * Iterates [JsonValue] over its siblings.
 * @param action A function(`Name of the sibling or a stringified integer if the `map` is an array`, `JsonValue representation of the sibling`)` -> `Unit`
 */
fun JsonValue.forEachSiblings(action: (String, JsonValue) -> Unit) = JsonFetcher.forEachSiblings(this, action)
/**
 * Iterates [JsonValue] over its siblings.
 * @param action A `function(index, `Name of the sibling or a stringified integer if the `map` is an array`, `JsonValue representation of the sibling`)` -> `Unit`
 */
fun JsonValue.forEachSiblingsIndexed(action: (Int, String, JsonValue) -> Unit) = JsonFetcher.forEachSiblingsIndexed(this, action)