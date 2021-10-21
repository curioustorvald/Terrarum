package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.App.printdbg
import java.io.File

data class TerrarumKeyLayout(
        val name: String,
        val symbols: Array<Array<String?>>?,
        val acceptChar: ((Int) -> String?)? = null
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

    private val lowLayers = HashMap<String, TerrarumKeyLayout>()

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
    }

    fun invoke() {}

    fun getLowLayerByName(name: String): TerrarumKeyLayout {
        return lowLayers[name.lowercase()]!!
    }

    fun getAllLowLayers(): List<String> {
        return lowLayers.keys.toList()
    }



    private fun parseKeylayoutFile(file: File): TerrarumKeyLayout {
        val src = file.readText(Charsets.UTF_8)
        val jsval = context.eval("js", "Object.freeze($src)")
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

}