package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.File

typealias IMECandidates = List<String>
typealias IMEOutput = String
typealias Keysyms = Array<Array<String?>>
typealias Keysymfun = Map<String, String>


data class TerrarumKeyLayout(
    val name: String,
    val capsMode: TerrarumKeyCapsMode,
    val symbols: Keysyms,
    val physicalLayout: String
)

enum class TerrarumKeyCapsMode {
    CAPS, SHIFT, BACK
}

data class TerrarumIME(
    val name: String,
    val config: TerrarumIMEConf,
    // (headkey, shiftin, altgrin, lowLayerKeysym)
    val acceptChar: (Int, Boolean, Boolean, String) -> Pair<IMECandidates, IMEOutput>,
    val backspace: () -> IMECandidates,
    val endCompose: () -> IMEOutput,
    val reset: () -> Unit,
    val composing: () -> Boolean
)

data class TerrarumIMEConf(
    val name: String,
    val copying: String,
    val lang: String,
    val candidates: TerrarumIMEViewCount,
    val symbols: Keysyms?,
    val symbolsfun: Keysymfun?,
    val mode: TerrarumIMEMode
)

enum class TerrarumIMEViewCount {
    NONE, ONE, MANY;

    fun toInt() = when (this) {
        NONE -> 0
        ONE -> 1
        MANY -> 10 // an hard-coded config
    }
}

enum class TerrarumIMEMode {
    CANDIDATES, REWRITE
}

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

    class LayoutNotFound(id: String): NullPointerException("Keyboard layout not found: $id")

    const val KEYLAYOUT_DIR = "assets/keylayout/"
    const val KEYLAYOUT_EXTENSION = "key"
    const val IME_EXTENSION = "ime"

    private val lowLayers = HashMap<String, TerrarumKeyLayout>()
    private val highLayers = HashMap<String, TerrarumIME>()

    private val context = org.graalvm.polyglot.Context.newBuilder("js")
            .allowHostAccess(org.graalvm.polyglot.HostAccess.EXPLICIT)
//            .allowHostClassLookup { it.equals("net.torvald.terrarum.gamecontroller.IMEProviderDelegate") }
            .allowHostClassLookup { false }
            .allowIO(false)
            .build()

    val icons = HashMap<String, TextureRegion>()

    init {
        context.getBindings("js").putMember("IMEProvider", IMEProviderDelegate(this))

        File(KEYLAYOUT_DIR).listFiles { file, s -> s.endsWith(".$KEYLAYOUT_EXTENSION") }.sortedBy { it.name }.forEach {
            printdbg(this, "Registering Low layer ${it.nameWithoutExtension.lowercase()}")
            registerLowLayer(it.nameWithoutExtension.lowercase(), parseKeylayoutFile(it))
        }

        File(KEYLAYOUT_DIR).listFiles { file, s -> s.endsWith(".$IME_EXTENSION") }.sortedBy { it.name }.forEach {
            printdbg(this, "Registering High layer ${it.nameWithoutExtension.lowercase()}")
            registerHighLayer(it.nameWithoutExtension.lowercase(), parseImeFile(it))
        }


        val iconSheet = TextureRegionPack("assets/graphics/gui/ime_icons_by_language.tga", 20, 20)
        val iconPixmap = Pixmap(Gdx.files.internal("assets/graphics/gui/ime_icons_by_language.tga"))
        for (k in 0 until iconPixmap.height step 20) {
            val langCode = StringBuilder()
            for (c in 0 until 20) {
                val x = c
                var charnum = 0
                for (b in 0 until 7) {
                    val y = k + b
                    if (iconPixmap.getPixel(x, y) and 255 != 0) {
                        charnum = charnum or (1 shl b)
                    }
                }
                if (charnum != 0) langCode.append(charnum.toChar())
            }

            if (langCode.isNotEmpty()) {
                printdbg(this, "Icon order #${(k+1) / 20} - icons[\"$langCode\"] = iconSheet.get(1, ${k/20})")
                icons["$langCode"] = iconSheet.get(1, k / 20).also { it.flip(false, false) }
            }
        }

        App.disposables.add(iconSheet)
        iconPixmap.dispose()
    }

    fun registerLowLayer(name: String, layout: TerrarumKeyLayout) {
        lowLayers[name] = layout
    }

    fun registerHighLayer(name: String, ime: TerrarumIME) {
        highLayers[name] = ime
    }

    @JvmStatic fun invoke() {}

    fun getLowLayerByName(name: String): TerrarumKeyLayout {
        return lowLayers[name.lowercase()]!!
    }

    fun getHighLayerByName(name: String): TerrarumIME {
        return highLayers[name.lowercase()]!!
    }

    fun getAllLowLayers(): List<String> {
        return lowLayers.keys.toList()
    }

    fun getAllHighLayers(): List<String> {
        return highLayers.keys.toList()
    }

    private fun String.toCapsMode() = when (this.lowercase()) {
        "caps" -> TerrarumKeyCapsMode.CAPS
        "shift" -> TerrarumKeyCapsMode.SHIFT
        "back" -> TerrarumKeyCapsMode.BACK
        else -> throw IllegalArgumentException("Unknown capslock mode: $this")
    }

    private fun String.toViewCount() = when (this.lowercase()) {
        "none" -> TerrarumIMEViewCount.NONE
        "one" -> TerrarumIMEViewCount.ONE
        "many" -> TerrarumIMEViewCount.MANY
        else -> throw IllegalArgumentException("Unknown candidates mode: $this")
    }

    fun parseKeylayoutFile(file: File): TerrarumKeyLayout {
        val src = file.readText(Charsets.UTF_8)
        val jsval = context.eval("js", "'use strict';Object.freeze($src)")
        val name = jsval.getMember("n").asString()
        val capsmode = jsval.getMember("capslock").asString().toCapsMode()
        val physicalLayout = try {
            jsval.getMember("l").asString().lowercase()
        }
        catch (e: NullPointerException) {
            "ansi"
        }

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

        return TerrarumKeyLayout(name, capsmode, out, physicalLayout)
    }

    private fun String.toCanditates(): List<String> =
            this.split(IMEDictionary.CAND_DELIM).mapNotNull { it.ifBlank { null } }
    private fun String.toIMEMode(): TerrarumIMEMode =
            when (this.lowercase()) {
                "rewrite" -> TerrarumIMEMode.REWRITE
                "candidates" -> TerrarumIMEMode.CANDIDATES
                else -> throw IllegalArgumentException("Unknown operation mode: $this")
            }

    fun parseImeFile(file: File): TerrarumIME {
        val code = file.readText(Charsets.UTF_8)
        val jsval = context.eval("js", "\"use strict\";(function(){$code})()")
        val name = jsval.getMember("n").asString()
        val candidatesCount = jsval.getMember("v").asString().toViewCount()
        val copying = jsval.getMember("c").asString()
        val keysyms = Array(256) { Array<String?>(4) { null } }
        var keysymtable: Keysymfun? = null
        val mode = jsval.getMember("m").asString().toIMEMode()
        val lang = jsval.getMember("l").asString()

        // keylayout is static (not affected by the Low Layout)
        if (jsval.hasMember("t")) {
            for (keycode in 0L until 256L) {
                val a = jsval.getMember("t").getArrayElement(keycode)
                if (!a.isNull) {
                    for (layer in 0L until 4L) {
                        if (a.arraySize > layer) {
                            val b = a.getArrayElement(layer)
                            if (!b.isNull) {
                                keysyms[keycode.toInt()][layer.toInt()] = b.asString()
                            }
                        }
                    }
                }
            }
        }
        // keylayout is dynamic (does affected by the Low Layout)
        else if (jsval.hasMember("tf")) {
            keysymtable = HashMap()
            val tf = jsval.getMember("tf")
            tf.memberKeys.forEach {
                keysymtable[it] = tf.getMember(it).asString()
            }
        }

        return TerrarumIME(
                name,
                TerrarumIMEConf(name, copying, lang, candidatesCount, if (keysymtable == null) keysyms else null, if (keysymtable == null) null else keysymtable, mode),
                { headkey, shifted, alted, lowLayerKeysym ->
                    val a = jsval.invokeMember("accept", headkey, shifted, alted, lowLayerKeysym)
//                    println(a.getArrayElement(0).asString().map { it.code.toString(16) })
                    a.getArrayElement(0).asString().toCanditates() to a.getArrayElement(1).let { if (it.isNull) "" else it.asString() }
                }, {
                    jsval.invokeMember("backspace").asString().toCanditates()
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