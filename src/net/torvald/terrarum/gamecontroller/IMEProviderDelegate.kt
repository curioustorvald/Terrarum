package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.App.printdbg
import net.torvald.util.SortedArrayList
import java.io.File
import java.io.FileReader

class IMEProviderDelegate(val ime: IME) {

    private val dictionaries = HashMap<String, IMEDictionary>()

    fun requestDictionary(filename: String): IMEDictionary {
        return dictionaries.getOrPut(filename) { IMEDictionary(filename) }
    }

}

class IMEDictionary(private val filename: String) {

    private val candidates = HashMap<String, String>(16384)
    private val keys = SortedArrayList<String>(16384)

    private var dictLoaded = false

    private fun loadDict() {
        val reader = FileReader(File("assets/keylayout/", filename))
        reader.forEachLine {
            val (key, value) = it.split(',')
            if (candidates.containsKey(key)) {
                candidates[key] += ",$value"
            }
            else {
                candidates[key] = value
                keys.add(key)
            }
        }

        printdbg(this, "Dictionary loaded: $filename")

        dictLoaded = true
    }

    init {
        loadDict() // loading the dict doesn't take too long so no need to do it lazily
    }

    operator fun get(key: String): String {
        //if (!dictLoaded) loadDict()

        val out = StringBuilder()
        var outsize = 0
        var index = keys.searchForInterval(key) { it }.second

        while (outsize < 10) {
            val keysym = keys[index]
            if (!keysym.startsWith(key)) break

            val outstr = ",${candidates[keysym]}"
            outsize += outstr.count { it == ',' }
            out.append(outstr)

            index += 1
        }

        return if (out.isNotEmpty()) out.substring(1) else ""
    }

}