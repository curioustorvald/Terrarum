package net.torvald.terrarum.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import java.io.FileReader
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

fun main() {
    val prop = Properties()
    prop.load(FileReader("./assets/keylayout/colemak.properties"))
    val layout = KeyboardLayout(prop)

    layout.drawToASCII()
}

class KeyboardLayout(layoutDesc: Properties) {
    data class Keycap(val width: Int, val label: String, val keycode: Int, val isSpace: Boolean, val isBlockedKey: Boolean)

    val keys = Array(5) { rowNum ->
        val rowNum = rowNum + 1
        val eachKeys = ArrayList<Keycap>()

        (layoutDesc["ROW$rowNum"] ?: "").toString().split(';').forEach {
            // text parsing machine
            val _size = StringBuilder()
            val _keyID = StringBuilder()
            var isSpace = false
            var delimiterLatched = false
            it.forEachIndexed { index, c ->
                if (!delimiterLatched && (c == 'q' || c == 's')) {
                    delimiterLatched = true

                    if (c == 's') isSpace = true
                }
                else if (!delimiterLatched) {
                    _size.append(c)
                }
                else {
                    _keyID.append(c)
                }
            }

            val size = _size.toString().toInt()
            val keyID = _keyID.toString()

            eachKeys.add(Keycap(size, toLabel(keyID), toKeycode(keyID), isSpace, keyID == "NULL"))
        }

        eachKeys.toTypedArray()
    }

    internal fun drawToASCII() {
        keys.forEach { row ->
            row.forEach {
                for (c in -1 until it.width - 1) {
                    if (c == -1 && !it.isSpace) {
                        print("|")
                    }
                    else {
                        if (it.isBlockedKey) {
                            print("#")
                        }
                        else {
                            try {
                                print(it.label[c])
                            }
                            catch (_: IndexOutOfBoundsException) {
                                print(" ")
                            }
                        }
                    }
                }

            }

            println("|")

        }
    }

    private fun toLabel(keyID: String) = when (keyID) {
        "" -> " "
        "MINUS" -> "-"
        "EQUAL" -> "="
        "QUOTE" -> "\""
        "SEMICOLON" -> ";"
        "LEFT_BRACKET" -> "["
        "RIGHT_BRACKET" -> "]"
        "COMMA" -> ","
        "PERIOD" -> "."
        "SLASH" -> "/"
        "BACKSLASH" -> "\\"
        else -> keyID
    }
    private fun toKeycode(keyID: String) = when (keyID) {
        else -> 10
    }
}

