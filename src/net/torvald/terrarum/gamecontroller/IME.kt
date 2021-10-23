package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.App.printdbg
import java.io.File

data class TerrarumKeyLayout(
        val name: String,
        val symbols: Array<Array<String?>>?
)

data class TerrarumInputMethod(
        val name: String,
        // (headkey, shiftin, altgrin)
        val acceptChar: (Int, Boolean, Boolean) -> Pair<String, String>, // Pair<Display Char, Output Char if any>
        val backspace: () -> String,
        val endCompose: () -> String,
        val reset: () -> Unit,
        val composing: () -> Boolean
)

/**
 * Key Layout File Structure for Low Layer:
 * - n: Displayed name of the keyboard layout
 * - t: Key symbols in 256R4C string array (C1: unshifted, C2: Shift, C3: AltGr, C4: Shift-AltGr)
 *
 * IME consists of two keyboard layers:
 * - Low layer: "english" keyboard (qwerty, colemak, etc), stateless
 * - High layer: chinese/japanese/korean/etc. keyboard, stateful
 *
 * Input to the IME Keyboard layout is always GDX/LWJGL3 keycode (only LWJGL3 offers OS-Keylayout-independent keycodes)
 *
 * Created by minjaesong on 2021-10-20.
 */
object IME {

    const val KEYLAYOUT_DIR = "assets/keylayout/"
    const val KEYLAYOUT_EXTENSION = "key"
    const val IME_EXTENSION = "ime"

    private val lowLayers = HashMap<String, TerrarumKeyLayout>()
    private val highLayers = HashMap<String, TerrarumInputMethod>()

    private val context = org.graalvm.polyglot.Context.newBuilder("js")
            .allowHostAccess(org.graalvm.polyglot.HostAccess.NONE)
            .allowHostClassLookup { false }
            .allowIO(false)
            .build()

    init {
        File(KEYLAYOUT_DIR).listFiles { file, s -> s.endsWith(".$KEYLAYOUT_EXTENSION") }.forEach {
            printdbg(this, "Registering Low layer ${it.nameWithoutExtension.lowercase()}")
            lowLayers[it.nameWithoutExtension.lowercase()] = parseKeylayoutFile(it)
        }

        File(KEYLAYOUT_DIR).listFiles { file, s -> s.endsWith(".$IME_EXTENSION") }.forEach {
            printdbg(this, "Registering High layer ${it.nameWithoutExtension.lowercase()}")
            highLayers[it.nameWithoutExtension.lowercase()] = parseImeFile(it)
        }
    }

    fun invoke() {}

    fun getLowLayerByName(name: String): TerrarumKeyLayout {
        return lowLayers[name.lowercase()]!!
    }

    fun getHighLayerByName(name: String): TerrarumInputMethod {
        return highLayers[name.lowercase()]!!
    }

    fun getAllLowLayers(): List<String> {
        return lowLayers.keys.toList()
    }

    fun getAllHighLayers(): List<String> {
        return highLayers.keys.toList()
    }


    private fun parseKeylayoutFile(file: File): TerrarumKeyLayout {
        val src = file.readText(Charsets.UTF_8)
        val jsval = context.eval("js", "'use strict';Object.freeze($src)")
        val name = jsval.getMember("n").asString()
        val out = Array(256) { Array<String?>(4) { null } }
        for (keycode in 0L until 256L) {
            val a = jsval.getMember("t").getArrayElement(keycode)
            if (!a.isNull) {
                for (layer in 0L until 4L) {
                    if (a.arraySize > layer) {
                        val b = a.getArrayElement(layer)
                        if (!b.isNull) {
                            out[keycode.toInt()][layer.toInt()] = b.asString()
                        }
                    }
                }
            }
        }

//        println("[IME] Test Keymap print for $name:"); for (keycode in 0 until 256) { print("$keycode:\t"); println(out[keycode].joinToString("\t")) }

        return TerrarumKeyLayout(name, out)
    }

    private fun parseImeFile(file: File): TerrarumInputMethod {
        val code = file.readText(Charsets.UTF_8)
        val jsval = context.eval("js", "\"use strict\";(function(){$code})()")
        val name = jsval.getMember("n").asString()


        return TerrarumInputMethod(name, { headkey, shifted, alted ->
            val a = jsval.invokeMember("accept", headkey, shifted, alted)
            a.getArrayElement(0).asString() to a.getArrayElement(1).asString()
        }, {
            jsval.invokeMember("backspace").asString()
        }, {
            jsval.invokeMember("end").asString()
        }, {
            jsval.invokeMember("reset")
        }, {
            jsval.invokeMember("composing").asBoolean()
        }
        )
    }

}