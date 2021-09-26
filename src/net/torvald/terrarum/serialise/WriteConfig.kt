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
        // KVHashMap
        jsoner.setSerializer(KVHashMap::class.java, object : Json.Serializer<KVHashMap> {
            override fun write(json: Json, obj: KVHashMap, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.hashMap.toSortedMap().forEach { (k, v) ->
                    json.writeValue(k,
                            if (v is Int) v as Int
                            else if (v is Double) v as Double
                            else if (v is IntArray) v as IntArray
                            else if (v is DoubleArray) v as DoubleArray
                            else if (v is Boolean) v as Boolean
                            else v.toString()
                    )
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

    operator fun invoke() {
        val writer = java.io.FileWriter(App.configDir, false)
        writer.write(jsoner.prettyPrint(App.gameConfig))
        writer.close()
    }

}