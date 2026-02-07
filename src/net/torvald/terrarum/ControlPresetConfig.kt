package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.utils.JsonFetcher
import java.io.File
import java.io.FileWriter

/**
 * Holds and manages the control preset configuration separately from the main config.
 * This allows users to easily share their control configurations via controls.json.
 *
 * Modules can register custom control labels using [registerModuleControl].
 *
 * Created by minjaesong on 2026-02-03.
 */
object ControlPresetConfig {

    private val configMap = KVHashMap()
    private val jsoner = Json(JsonWriter.OutputType.json)

    // Default values for built-in controls
    private val defaults = hashMapOf<String, Any>(
        "control_preset_keyboard" to "WASD",

        // Keyboard controls (ESDF defaults matching DefaultConfig)
        "control_key_up" to Input.Keys.E,
        "control_key_left" to Input.Keys.S,
        "control_key_down" to Input.Keys.D,
        "control_key_right" to Input.Keys.F,
        "control_key_jump" to Input.Keys.SPACE,
        "control_key_movementaux" to Input.Keys.A,
        "control_key_inventory" to Input.Keys.Q,
        "control_key_interact" to Input.Keys.R,
        "control_key_discard" to Input.Keys.T,
        "control_key_close" to Input.Keys.C,
        "control_key_zoom" to Input.Keys.Z,
        "control_key_gamemenu" to Input.Keys.TAB,
        "control_key_crafting" to Input.Keys.W,
        "control_key_quicksel" to Input.Keys.SHIFT_LEFT,
        "control_key_toggleime" to Input.Keys.ALT_RIGHT,

        // Mouse controls
        "control_mouse_primary" to Input.Buttons.LEFT,
        "control_mouse_secondary" to Input.Buttons.RIGHT,
        "control_mouse_quicksel" to Input.Buttons.MIDDLE,

        // Array-type controls
        "control_key_quickslots" to ((Input.Keys.NUM_1..Input.Keys.NUM_9) + arrayOf(Input.Keys.NUM_0)).map { 1.0 * it }.toDoubleArray(),
        "control_key_quickselalt" to intArrayOf(Input.Keys.BACKSPACE, Input.Keys.CONTROL_LEFT, Input.Keys.BACKSLASH).map { 1.0 * it }.toDoubleArray(),
    )

    init {
        // Set up JSON serialiser for KVHashMap
        jsoner.ignoreUnknownFields = true
        jsoner.setUsePrototypes(false)
        jsoner.setIgnoreDeprecated(false)
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

    /**
     * Initialises the control preset config with defaults, then loads from file.
     * Should be called during App initialisation, BEFORE modules load.
     */
    @JvmStatic
    fun initialise() {
        // Populate with defaults
        defaults.forEach { (k, v) -> configMap[k] = v }

        // Load from file if exists
        loadFromFile()
    }

    /**
     * Registers a custom control for a module.
     * @param moduleName The module's identifier (directory name)
     * @param device One of "key", "mouse" (not "gamepad")
     * @param label The control label (e.g., "special_action")
     * @param defaultValue The default key/button code
     */
    fun registerModuleControl(moduleName: String, device: String, label: String, defaultValue: Int) {
        require(device in listOf("key", "mouse")) { "Device must be 'key' or 'mouse', got '$device'" }
        val key = "${moduleName}:control_${device}_$label"
        if (!configMap.hasKey(key)) {
            configMap[key] = defaultValue
        }
    }

    /**
     * Returns true if this key should be handled by ControlPresetConfig instead of App.gameConfig
     */
    @JvmStatic
    fun isControlKey(key: String): Boolean {
        val k = key.lowercase()
        return k.startsWith("control_key_") || k.startsWith("control_mouse_") || k == "control_preset_keyboard" ||
               (k.contains(":control_key_") || k.contains(":control_mouse_"))
    }

    @JvmStatic
    fun getString(key: String): String? = configMap.getAsString(key.lowercase()) ?: defaults[key.lowercase()] as? String

    @JvmStatic
    fun getInt(key: String): Int {
        val k = key.lowercase()
        val value = configMap[k] ?: defaults[k]
        return when (value) {
            is Int -> value
            is Double -> value.toInt()
            else -> -1
        }
    }

    @JvmStatic
    fun getIntArray(key: String): IntArray {
        val k = key.lowercase()
        val arr = configMap[k] ?: defaults[k]
        return when (arr) {
            is DoubleArray -> arr.map { it.toInt() }.toIntArray()
            is IntArray -> arr
            else -> intArrayOf()
        }
    }

    @JvmStatic
    fun getDoubleArray(key: String): DoubleArray {
        val k = key.lowercase()
        val arr = configMap[k] ?: defaults[k]
        return when (arr) {
            is DoubleArray -> arr
            is IntArray -> arr.map { it.toDouble() }.toDoubleArray()
            else -> doubleArrayOf()
        }
    }

    @JvmStatic
    fun get(key: String): Any? {
        val k = key.lowercase()
        return configMap[k] ?: defaults[k]
    }

    @JvmStatic
    fun set(key: String, value: Any) {
        configMap[key.lowercase()] = value
    }

    fun containsKey(key: String): Boolean {
        val k = key.lowercase()
        return configMap.hasKey(k) || defaults.containsKey(k)
    }

    /**
     * Returns the key set of configured controls (excludes defaults not yet written)
     */
    val keySet: Set<Any>
        get() = configMap.keySet

    private fun loadFromFile() {
        val file = File(App.controlPresetDir)
        if (!file.exists()) return

        try {
            val json = JsonFetcher(App.controlPresetDir)
            var entry: JsonValue? = json.child
            while (entry != null) {
                configMap[entry.name] = when {
                    entry.isArray -> entry.asDoubleArray()
                    entry.isDouble -> entry.asDouble()
                    entry.isBoolean -> entry.asBoolean()
                    entry.isLong -> entry.asInt()
                    else -> entry.asString()
                }
                entry = entry.next
            }
        } catch (e: Exception) {
            System.err.println("[ControlPresetConfig] Failed to load controls.json: ${e.message}")
        }
    }

    @JvmStatic
    fun save() {
        try {
            val writer = FileWriter(App.controlPresetDir, false)
            writer.write(jsoner.prettyPrint(configMap))
            writer.close()
        } catch (e: Exception) {
            System.err.println("[ControlPresetConfig] Failed to save controls.json: ${e.message}")
        }
    }

    /**
     * Migrates control settings from App.gameConfig to this config.
     * Called once during first run with new system.
     */
    @JvmStatic
    fun migrateFromGenericConfig() {
        val keysToMigrate = App.gameConfig.keySet.filter { key ->
            val k = (key as String).lowercase()
            k.startsWith("control_key_") || k.startsWith("control_mouse_") || k == "control_preset_keyboard"
        }

        if (keysToMigrate.isEmpty()) return

        keysToMigrate.forEach { key ->
            val k = key as String
            configMap[k] = App.gameConfig[k]!!
        }

        save()
        println("[ControlPresetConfig] Migrated ${keysToMigrate.size} control settings from config.json")
    }
}
