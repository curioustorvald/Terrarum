package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.App.printdbg
import net.torvald.util.SortedArrayList
import org.graalvm.polyglot.HostAccess
import java.io.File
import java.io.FileReader

class IMEProviderDelegate(val ime: IME) {

    private val dictionaries = HashMap<String, IMEDictionary>()

    @HostAccess.Export
    fun requestDictionary(filename: String): IMEDictionary {
        return dictionaries.getOrPut(filename) { IMEDictionary(filename) }
    }

}

class IMEDictionary(private val filename: String) {

    companion object {
        const val CAND_DELIM = '\u001E'
    }

    private val candidates = HashMap<String, String>(16384)
    private val keys = SortedArrayList<String>(16384) // keys on the .han file are absofreakinlutely not sorted

    private var dictLoaded = false

    private fun loadDict() {
        val reader = FileReader(File("assets/keylayout/", filename), Charsets.UTF_8)
        reader.forEachLine {
            if (it.contains(',')) {
                val (key, value) = it.split(',')
                if (candidates.containsKey(key)) {
                    candidates[key] += "$CAND_DELIM$value"
                }
                else {
                    candidates[key] = value
                    keys.add(key)
                }
            }
        }

        printdbg(this, "Dictionary loaded: $filename")

        dictLoaded = true
    }

    init {
        loadDict() // loading the dict doesn't take too long so no need to do it lazily
    }

    @HostAccess.Export
    fun getCangjie(key: String): String {
        //if (!dictLoaded) loadDict()

        val out = StringBuilder()
        var outsize = 0
        var index = keys.searchForInterval(key) { it }.second

        val allRelevantKeys = ArrayList<String>() // oh, oha, ohag, ohbt, ohby, ...
        for (k in 0 until 400) { // lookahead for length-first sorting, longest lookahead possible is around 300, so I'll give it 400
            if (index + k >= keys.size) break
            val keysym = keys[index + k]
            if (!keysym.startsWith(key)) break
            allRelevantKeys.add(keysym)
        }

//        printdbg(this, "lookup key: $key")

        // sort allRelevantKeys so that short sequences come first
        // e.g. oh, oha, ohg, ohj, ohn, ohq, ohag, ohbt, ...
        allRelevantKeys.sortWith { it, other ->
            if (it.length == other.length) it.compareTo(other)
            else it.length.compareTo(other.length)
        }

//        printdbg(this, "predictions: (${allRelevantKeys.size}) ${allRelevantKeys.joinToString()}")

        index = 0 // now this is an index for the allRelevantKeys
        while (outsize < 10 && index < allRelevantKeys.size) {
            val keysym = allRelevantKeys[index]
            if (!keysym.startsWith(key)) break

            val outstr = "$CAND_DELIM${candidates[keysym]}"
            outsize += outstr.count { it == CAND_DELIM }
            out.append(outstr)

            index += 1
        }

        return if (out.isNotEmpty()) out.substring(1) else ""
    }

}