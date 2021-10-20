package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.App.printdbg

typealias TerrarumKeyLayout = Array<Array<String?>>

/**
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
    const val KEYLAYOUT_EXTENSION = ".key"

    private val cached = HashMap<String, TerrarumKeyLayout>()

    private val context = org.graalvm.polyglot.Context.newBuilder("js")
            .allowHostAccess(org.graalvm.polyglot.HostAccess.NONE)
            .allowHostClassLookup { false }
            .allowIO(false)
            .build()

    fun getLowLayerByName(name: String): TerrarumKeyLayout {
        return cached.getOrPut(name) { parseKeylayoutFile("$KEYLAYOUT_DIR$name$KEYLAYOUT_EXTENSION") }
    }



    private fun parseKeylayoutFile(path: String): TerrarumKeyLayout {
        val file = Gdx.files.internal(path)
        val src = file.readString("UTF-8")
        val jsval = context.eval("js", src)
        val out = Array(256) { Array<String?>(4) { null } }
        for (keycode in 0L until 256L) {
            val a = jsval.getArrayElement(keycode)
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

        //println("[IME] Test Keymap print:"); for (keycode in 0 until 256) { print("$keycode:\t"); println(out[keycode].joinToString("\t")) }

        return out
    }

}