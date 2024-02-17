package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.App
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.Principii
import net.torvald.terrarum.utils.JsonFetcher

/**
 * Created by minjaesong on 2021-09-19.
 */
object WriteConfig {

    private val jsoner = Json(JsonWriter.OutputType.json)

    init {
        jsoner.ignoreUnknownFields = true
        jsoner.setUsePrototypes(false)
        jsoner.setIgnoreDeprecated(false)

        // KVHashMap
        jsoner.setSerializer(KVHashMap::class.java, object : Json.Serializer<KVHashMap> {
            override fun write(json: Json, obj: KVHashMap, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.hashMap.toSortedMap().forEach { (k, v) ->
                    json.writeValue(k, v)
                }
                json.writeObjectEnd()
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): KVHashMap {
                val map = KVHashMap()
                JsonFetcher.forEachSiblings(jsonData) { key, obj ->
                    map[key] = json.readValue(null, obj)
                }
                return map
            }
        })

    }

    /*fun getJson(): String {
        val sb = StringBuilder()

        App.gameConfig.hashMap.toSortedMap().forEach { (k, v) ->
            sb.append("$k:")

            when (v) {
                is DoubleArray -> { sb.append("[${v.joinToString(",")}]") }
                is IntArray -> { sb.append("[${v.joinToString(",")}]") }
                is Array<*> -> { sb.append("[${v.joinToString(",")}]") }
                else -> { sb.append("$v") }
            }

            sb.append("\n")
        }

        return "{\n$sb}"
    }*/

    operator fun invoke() {
        val writer = java.io.FileWriter(App.configDir, false)
        //writer.write(getJson())
        writer.write(jsoner.prettyPrint(App.gameConfig))
        writer.close()
    }

}

/**
 * Created by minjaesong on 2024-02-17.
 */
object TryResize {
    private val gameConfig = KVHashMap()

    private var doResize = false

    fun pre() {
        // read from disk and build config from it
        val oldJsonMap = JsonFetcher.invoke(App.configDir)
        // make config
        var entry: JsonValue? = oldJsonMap.child
        while (entry != null) {
            setToGameConfigForced(entry, null)
            entry = entry.next
        }


        // check for discrepancy
        listOf("screenwidth", "screenheight").forEach {
            if (gameConfig.getAsInt(it) != App.getConfigInt(it))
                doResize = doResize or true
        }
    }

    private fun setToGameConfigForced(value: JsonValue, modName: String?) {
        gameConfig[if ((modName == null)) value.name else modName + ":" + value.name] =
            if (value.isArray) value.asDoubleArray() else if (value.isDouble) value.asDouble() else if (value.isBoolean) value.asBoolean() else if (value.isLong) value.asInt() else value.asString()
    }

    operator fun invoke() {
        // it just wouldn't work, the only way to make it work will be calling game restart from the launcher
    }

}
