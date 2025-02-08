package net.torvald.terrarum

import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

typealias ItemValue = KVHashMap

/**
 * Supported Value Types:
 * - Int
 * - Double (`getAsFloat()` first retrieves the Double value then casts to Float)
 * - Boolean
 * - String
 *
 * Created by minjaesong on 2015-12-30.
 */
open class KVHashMap {

    constructor() {
        hashMap = HashMap<String, Any>()
    }

    protected constructor(newMap: HashMap<String, Any>) {
        hashMap = newMap
    }

    var hashMap: HashMap<String, Any>

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

    fun setBlob(key: String, value: ByteArray) {
        val filename0 = hashMap.getOrPut(key.toLowerCase()) { "blob://${UUID.randomUUID()}" } as String
        if (filename0.startsWith("blob://")) {
            Terrarum.getSharedSaveFiledesc(filename0.removePrefix("blob://")).let {
                if (!it.exists()) it.createNewFile()
                it.writeBytes(value) // will overwrite whatever's there
            }
        }
        else throw TypeCastException("ActorValue is not blob")
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
        val value = get(key) ?: return null
        return value as Int
    }

    fun getAsDouble(key: String): Double? {
        val value = get(key) ?: return null
        if (value is Int) return value.toDouble()
        return value as Double
    }

    fun getAsFloat(key: String): Float? {
        val value = get(key) ?: return null
        if (value is Float) return value
        return getAsDouble(key)?.toFloat()
    }

    fun getAsString(key: String): String? {
        val value = get(key) ?: return null
        return value as String
    }

    fun getAsBoolean(key: String): Boolean? {
        val value = get(key) ?: return null
        return value as Boolean
    }

    fun getAsBlob(key: String): ByteArray? {
        val uri = getAsString(key) ?: return null
        if (uri.startsWith("blob://")) {
            val filename = uri.removePrefix("blob://")
            val file = Terrarum.getSharedSaveFiledesc(filename)
            if (file.exists())
                return file.readBytes()
            else
                throw IOException("Blob not found")
        }
        else throw TypeCastException("ActorValue is not blob")
    }

    fun hasKey(key: String) = hashMap.containsKey(key)

    val keySet: Set<Any>
        get() = hashMap.keys

    open fun remove(key: String) {
        if (hashMap[key] != null) {
            val value = hashMap[key]!!
            hashMap.remove(key, value)

            (value as? String)?.let {
                if (it.startsWith("blob://")) {
                    val filename = it.removePrefix("blob://")
                    Terrarum.getSharedSaveFiledesc(filename).delete()
                }
            }
        }
    }

    open fun clone(): KVHashMap {
        val cloneOfMap = hashMap.clone() as HashMap<String, Any>
        return KVHashMap(cloneOfMap)
    }
}