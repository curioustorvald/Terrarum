package net.torvald.terrarum.gamecontroller

import java.io.File
import java.io.FileReader

class IMEProviderDelegate(val ime: IME) {

    private val dictionaries = HashMap<String, IMEDictionary>()

    fun requestDictionary(filename: String): IMEDictionary {
        return dictionaries.getOrPut(filename) { IMEDictionary(filename) }
    }

}

class IMEDictionary(filename: String) {

    private val candidates = HashMap<String, String>()

    init {
        val reader = FileReader(File("assets/keylayout/", filename))
        reader.forEachLine {
            val (key, value) = it.split(',')
            if (candidates.containsKey(key)) {
                candidates[key] += ",$value"
            }
            else {
                candidates[key] = value
            }
        }
    }

    operator fun get(key: String): String = candidates[key] ?: ""

}