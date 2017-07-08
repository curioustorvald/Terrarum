package net.torvald.terrarum

import com.google.gson.JsonPrimitive
import net.torvald.terrarum.gameactors.Actor
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap

typealias ItemValue = KVHashMap
typealias GameConfig = KVHashMap

/**
 * Created by minjaesong on 15-12-30.
 */
open class KVHashMap {

    constructor() {
        hashMap = HashMap<String, Any>()
    }

    protected constructor(newMap: HashMap<String, Any>) {
        hashMap = newMap
    }

    protected var hashMap: HashMap<String, Any>

    /**
     * Add key-value pair to the configuration table.
     * If key does not exist on the table, new key will be generated.
     * If key already exists, the value will be overwritten.

     * @param key case insensitive
     * *
     * @param value
     */
    open operator fun set(key: String, value: Any) {
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

        if (value == null) return null

        if (value is JsonPrimitive)
            return value.asInt

        return value as Int
    }

    fun getAsDouble(key: String): Double? {
        val value = get(key)

        if (value == null) return null

        if (value is Int)
            return value.toDouble()
        else if (value is JsonPrimitive)
            return value.asDouble

        return value as Double
    }

    fun getAsFloat(key: String): Float? {
        return getAsDouble(key)?.toFloat()
    }

    fun getAsString(key: String): String? {
        val value = get(key)

        if (value == null) return null

        if (value is JsonPrimitive)
            return value.asString

        return value as String
    }

    fun getAsBoolean(key: String): Boolean? {
        val value = get(key)

        if (value == null) return null

        if (value is JsonPrimitive)
            return value.asBoolean

        return value as Boolean
    }

    fun hasKey(key: String) = hashMap.containsKey(key)

    val keySet: Set<Any>
        get() = hashMap.keys

    open fun remove(key: String) {
        if (hashMap[key] != null) {
            hashMap.remove(key, hashMap[key]!!)
        }
    }

    open fun clone(): KVHashMap {
        val cloneOfMap = hashMap.clone() as HashMap<String, Any>
        return KVHashMap(cloneOfMap)
    }

}