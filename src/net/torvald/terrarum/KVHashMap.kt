package net.torvald.terrarum

import com.google.gson.JsonPrimitive
import java.util.*
import java.util.function.Consumer

typealias ActorValue = KVHashMap
typealias ItemValue = KVHashMap
typealias GameConfig = KVHashMap

/**
 * Created by minjaesong on 15-12-30.
 */
class KVHashMap {

    private val hashMap = HashMap<String, Any>()

    /**
     * Add key-value pair to the configuration table.
     * If key does not exist on the table, new key will be generated.
     * If key already exists, the value will be overwritten.

     * @param key case insensitive
     * *
     * @param value
     */
    operator fun set(key: String, value: Any) {
        hashMap.put(key.toLowerCase(), value)
    }

    /**
     * Get value using key from configuration table.

     * @param key case insensitive
     * *
     * @return Object value
     */
    operator fun get(key: String): Any? {
        return hashMap[key.toLowerCase()]
    }

    fun getAsInt(key: String): Int? {
        val value = get(key)

        if (value is JsonPrimitive)
            return value.asInt

        try {
            return value as Int
        }
        catch (e: ClassCastException) {
            return null
        }
    }

    fun getAsDouble(key: String): Double? {
        val value = get(key)

        if (value is Int)
            return value.toDouble()
        else if (value is JsonPrimitive)
            return value.asDouble

        try {
            return value as Double
        }
        catch (e: ClassCastException) {
            return null

        }
    }

    fun getAsString(key: String): String? {
        val value = get(key)

        if (value is JsonPrimitive)
            return value.asString

        try {
            return value as String
        }
        catch (e: ClassCastException) {
            return null
        }
    }

    fun getAsBoolean(key: String): Boolean? {
        val value = get(key)

        if (value is JsonPrimitive)
            return value.asBoolean

        try {
            return value as Boolean
        }
        catch (e: ClassCastException) {
            return null
        }
    }

    fun hasKey(key: String) = hashMap.containsKey(key)

    val keySet: Set<Any>
        get() = hashMap.keys

    fun remove(key: String) {
        if (hashMap[key] != null)
            hashMap.remove(key, hashMap[key]!!)
    }

}