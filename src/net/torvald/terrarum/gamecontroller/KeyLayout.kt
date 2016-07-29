package net.torvald.terrarum.gamecontroller

import net.torvald.JsonFetcher
import java.util.*

/**
 * Created by minjaesong on 16-07-28.
 */
object KeyLayout {

    /**
     * HashMap<identifier: String, KeyLayoutClass>
     */
    val layouts: HashMap<String, KeyLayoutClass>

    init {
        layouts = HashMap<String, KeyLayoutClass>()

        val json = JsonFetcher("./res/keylayout.json")
        json.entrySet().forEach { it ->
            layouts.put(
                    it.key,
                    KeyLayoutClass(
                            it.value.asJsonObject.get("layout").asString,
                            it.value.asJsonObject.get("name").asString,
                            it.value.asJsonObject.get("capslock").asString
                    )
            )
        }
    }

}

class KeyLayoutClass(layoutString: String, val layoutName: String, capsMode: String) {
    val disposition = intArrayOf(
            // alphanumeric
             2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,
              16,17,18,19,20,21,22,23,24,25,26,27,
               30,31,32,33,34,35,36,37,38,39,40,
                 44,45,46,47,48,49,50,51,52,53,
            // control keys
                                                14, // back
            15, // tab
            58,                                 28, // capslock, return/enter
            42, // lshift
            29,              57 // lcontrol, space
    )
    val engraving = ArrayList<String>(disposition.size)

    init {
        /* ================== *
         * parse layoutString *
         * ================== */
        // zero-fill engraving
        for (i in 1..disposition.size) engraving.add("")

        // Backspace
        engraving[disposition.indexOf(14)] = "BACK"
        // Tab
        engraving[disposition.indexOf(15)] = "TAB"
        // Capslock
        engraving[disposition.indexOf(58)] = "CAPS"
        // Enter
        engraving[disposition.indexOf(28)] = "ENTER"
        // LShift
        engraving[disposition.indexOf(42)] = "SHIFT"
        // Control
        engraving[disposition.indexOf(29)] = "CTRL"
        // Space
        engraving[disposition.indexOf(57)] = "SPACE"

        // alphanumeric
        for (i in 0..layoutString.length - 1) {
            engraving[disposition.indexOf(i)] = layoutString[i].toString()
        }
    }

    fun codeToLabel(code: Int) = engraving[disposition.indexOf(code)]

    fun labelToCode(char: Char) = disposition[engraving.indexOf(char.toUpperCase().toString())]
}