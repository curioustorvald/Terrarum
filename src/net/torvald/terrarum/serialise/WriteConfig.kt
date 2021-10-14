package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.App
import net.torvald.terrarum.KVHashMap
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
                JsonFetcher.forEach(jsonData) { key, obj ->
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