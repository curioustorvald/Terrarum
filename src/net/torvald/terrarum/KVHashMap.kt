package net.torvald.terrarum

import com.google.gson.JsonPrimitive
import java.util.*

/**
 * Created by minjaesong on 16-03-19.
 */
open class KVHashMap {

    private val hashMap: HashMap<String, Any>

    init {
        hashMap = HashMap<String, Any>()
    }

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

        return get(key) as Int?
    }

    fun getAsFloat(key: String): Float? {
        val value = get(key)

        if (value is Int)
            return value.toFloat()
        else if (value is JsonPrimitive)
            return value.asFloat

        return value as Float?
    }

    fun getAsDouble(key: String): Double? {
        val value = get(key)

        if (value is Int)
            return value.toDouble()
        else if (value is JsonPrimitive)
            return value.asDouble

        return value as Double?
    }

    fun getAsString(key: String): String? {
        val value = get(key)

        if (value is JsonPrimitive)
            return value.asString

        return value as String?
    }

    fun getAsBoolean(key: String): Boolean? {
        val value = get(key)

        if (value is JsonPrimitive)
            return value.asBoolean

        return value as Boolean?
    }

    fun hasKey(key: String): Boolean {
        return hashMap.containsKey(key)
    }

    val keySet: Set<Any>
        get() = hashMap.keys

}