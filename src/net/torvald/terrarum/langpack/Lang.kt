package net.torvald.terrarum.langpack

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.tail
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.unicode.getKeycapPC
import net.torvald.unicode.getMouseButton
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class LangObject(val key: String, val fromLang: Boolean) {
    fun get() = if (fromLang) Lang[key] else key
}

/**
 * Created by minjaesong on 2016-01-22.
 */
object Lang {

    /**
     * Get record by its STRING_ID
     *
     * HashMap<"$key_$language", Value>
     *
     *     E.g. langpack["MENU_LANGUAGE_THIS_fiFI"]
     */
    private val langpack = HashMap<String, String>()
    private val FALLBACK_LANG_CODE = "en"

    private val HANGUL_SYL_START = 0xAC00

    val languageList = HashSet<String>()

    val POLYGLOT_VERSION = "100"
    private val PREFIX_POLYGLOT = "Polyglot-${POLYGLOT_VERSION}_"
    private val PREFIX_NAMESET = "nameset_"

    private val HANGUL_POST_INDEX_ALPH = intArrayOf(// 0: 는, 가, ...  1: 은, 이, ...
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val HANGUL_POST_RO_INDEX_ALPH = intArrayOf(// 0: 로  1: 으로
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    private val ENGLISH_WORD_NORMAL_PLURAL = arrayOf("photo", "demo")

    private val FRENCH_WORD_NORMAL_PLURAL = arrayOf("bal", "banal", "fatal", "final")

    init {
        // load base langs
        load(File("./assets/locales/"))
    }


    @JvmStatic operator fun invoke() { /* dummy method for manual initialisation */ }

    fun load(localesDir: File) {
        printdbg(this, "Loading languages from $localesDir")

        // get all of the languages installed
        localesDir.listFiles().filter { it.isDirectory }.forEach { languageList.add(it.name) }

        // temporary filter
        languageList.remove("jaJPysi")

        for (lang in languageList) {
            printdbg(this, "Loading langpack from $localesDir/$lang/")

            val langFileListFiles = File("$localesDir/$lang/").listFiles()

            langFileListFiles?.forEach {
                // not a polyglot
                if (!it.name.startsWith("Polyglot") && it.name.endsWith(".json")) {
                    processRegularLangfile(it, lang)
                }
                else if (it.name.startsWith("Polyglot") && it.name.endsWith(".json")) {
                    processPolyglotLangFile(it, lang)
                }
                // else, ignore
            }

        }
    }

    private fun processRegularLangfile(file: File, lang: String) {
        val json = JsonFetcher(file)
        /*
         * Terrarum langpack JSON structure is:
         *
         * (root object)
         *      "<<STRING ID>>" = "<<LOCALISED TEXT>>"
         */
        //println(json.entrySet())
        JsonFetcher.forEachSiblings(json) { key, value ->
            langpack.put("${key}_$lang", value.asString().trim())
        }

    }

    private fun processPolyglotLangFile(file: File, lang: String) {
        val json = JsonFetcher(file)
        /*
         * Polyglot JSON structure is:
         *
         * (root object)
         *      "resources": object
         *          "polyglot": object
         *              (polyglot meta)
         *          "data": array
         *             [0]: object
         *                  n = "CONTEXT_CHARACTER_CLASS"
         *                  s = "Class"
         *             [1]: object
         *                  n = "CONTEXT_CHARACTER_DELETE"
         *                  s = "Delecte Character"
         *             (the array continues)
         *
         */
        JsonFetcher.forEachSiblings(json.get("resources").get("data")) { _, entry ->
            langpack.put(
                    "${entry.getString("n")}_$lang",
                    entry.getString("s").trim()
            )
        }

    }

    private val bindOp = ">>="

    fun getOrNull(key: String?, capitalise: Boolean = false) =
        if (key == null) null else get(key, capitalise).let {
            if (it.startsWith("$")) null else it
        }

    /**
     * Syntax example:
     *
     * - `BLOCK_AIR` – Prints out `Lang.get("BLOCK_AIR")`
     * - `BLOCK_AIR>>=BLOCK_WALL_NAME_TEMPLATE` – Prints out `Formatter().format(Lang.get("BLOCK_WALL_NAME_TEMPLATE"), Lang.get("BLOCK_AIR")).toString()`
     */
    operator fun get(key: String, capitalise: Boolean = false): String {
        fun getstr(s: String) = getByLocale(s, App.GAME_LOCALE, capitalise) ?: getByLocale(s, FALLBACK_LANG_CODE, capitalise) ?: "$$s"

        decodeCache[App.GAME_LOCALE]?.get("$key+$capitalise").let {
            if (it != null) {
                return it
            }
            else {
                val args = key.split(bindOp).filter { it.isNotBlank() }.map { it.trim() }
                if (args.isEmpty()) return ""

                val sb = StringBuilder()
                val formatter = Formatter(sb)

                sb.append(getstr(args[0]))
                args.subList(1, args.size).forEach {
                    val oldstr = sb.toString().trim()
                    sb.clear()
                    formatter.format(getstr(it), oldstr)
                }

                if (decodeCache[App.GAME_LOCALE] == null) {
                    decodeCache[App.GAME_LOCALE] = HashMap()
                }
                decodeCache[App.GAME_LOCALE]!!["$key+$capitalise"] = sb.toString().trim()

                return sb.toString().trim()
            }
        }

    }

    fun getAndUseTemplate(key: String, capitalise: Boolean = false, vararg arguments: Any?): String? {
        var raw = getOrNull(key, capitalise) ?: return null

        arguments.forEachIndexed { index, it0 ->
            val it = if (capitalise) it0.toString().capitalize() else it0.toString()
            raw = raw.replace("{${index}}", it.trim())
        }
        return raw.trim()
    }

    /**
     * @param localecode int the form of "en", "de" or "daDK" or something
     */
    private fun getJavaLocaleFromTerrarumLocaleCode(localecode: String): Locale {
        val localecode = localecode.substring(0 until minOf(4, localecode.length))
        val lang = localecode.substring(0..1)
        val country = if (localecode.length == 4) localecode.substring(2..3) else null
        return if (country == null) Locale(lang) else Locale(lang, country)
    }

    private val capCache = HashMap<String/*Locale*/, HashMap<String/*Key*/, String/*Text*/>>()
    private val decodeCache = HashMap<String/*Locale*/, HashMap<String/*Key*/, String/*Text*/>>()

    private fun CAP(key: String, locale: String): String? {
        val ret = langpack["${key}_$locale"] ?: return null

        if (!capCache.containsKey(locale))
            capCache[locale] = HashMap<String, String>()

        if (!capCache[locale]!!.containsKey(key)) {
            capCache[locale]!![key] = TitlecaseConverter(ret, locale).trim()
        }

        return capCache[locale]!![key]!!
    }

    private fun NOCAP(key: String, locale: String): String? {
        return langpack["${key}_$locale"] ?: return null
    }


    private val tagRegex = Regex("""\{[A-Z]+:[0-9A-Za-z_\- ]+\}""")

    /**
     * Does NOT parse the bind operators, but DO parse the precomposed tags
     */
    fun getByLocale(key: String, locale: String, capitalise: Boolean = false): String? {
        val s = if (capitalise) CAP(key, locale) else NOCAP(key, locale)

        if (s == null) return null

        val fetchedS = if (locale.startsWith("bg"))
            "${App.fontGame.charsetOverrideBulgarian}$s${App.fontGame.charsetOverrideDefault}"
        else if (locale.startsWith("sr"))
            "${App.fontGame.charsetOverrideSerbian}$s${App.fontGame.charsetOverrideDefault}"
        else
            s

        // apply {DOMAIN:argument} form of template
        var ret = "$fetchedS" // make copy of the str
        tagRegex.findAll(fetchedS).forEach {
            val matched0 = it.groupValues[0]
            val matched = matched0.substring(1 until matched0.lastIndex) // strip off the brackets
            val mode = matched.substringBefore(':')
            val key = matched.substringAfter(':')

            val resolved = when (mode) {
                "MOUSE" -> { // cognates to gdx.Input.Button
                    getMouseButton(App.getConfigInt(key)).toString()
                }
                "KEYCAP" -> { // cognates to gdx.Input.Keys
                    getKeycapPC(App.getConfigInt(key)).toString()
                }
                "CONFIG" -> {
                    App.getConfigMaster(key).toString()
                }
                else -> matched0
            }

            ret = ret.replace(matched0, resolved)
        }
        return ret.trim()
    }

    private fun String.getEndTag() = this.split("_").last()

    fun pluraliseLang(key: String, count: Int): String {
        return if (count > 1) get(key + "_PLURAL") else get(key)
    }

    fun pluralise(word: String, count: Int): String {
        if (count < 2) return word

        when (App.GAME_LOCALE) {
            "fr" -> {
                if (Arrays.binarySearch(FRENCH_WORD_NORMAL_PLURAL, word) >= 0) {
                    return word + "s"
                }
                if (word.endsWith("al") || word.endsWith("au") || word.endsWith("eu") || word.endsWith("eau")) {
                    return word.substring(0, word.length - 2) + "ux"
                }
                else if (word.endsWith("ail")) {
                    return word.substring(0, word.length - 3) + "ux"
                }
                else {
                    return word + "s"
                }
            }
            "en" -> {
                if (Arrays.binarySearch(ENGLISH_WORD_NORMAL_PLURAL, word) >= 0) {
                    return word + "s"
                }
                else if (word.endsWith("f")) {
                    // f -> ves
                    return word.substring(0, word.length - 2) + "ves"
                }
                else if (word.endsWith("o") || word.endsWith("z")) {
                    // o -> oes
                    return word + "es"
                }
                else {
                    return word + "s"
                }
            }
            else -> {
                if (Arrays.binarySearch(ENGLISH_WORD_NORMAL_PLURAL, word) >= 0) {
                    return word + "s"
                }
                else if (word.endsWith("f")) {
                    return word.substring(0, word.length - 2) + "ves"
                }
                else if (word.endsWith("o") || word.endsWith("z")) {
                    return word + "es"
                }
                else {
                    return word + "s"
                }
            }
        }
    }

    fun getHangulJosa(word: String, josa1: String, josa2: String): String {
        val lastChar = getLastChar(word)
        val isIrregular = josa1.startsWith("로") && josa2.startsWith("으로")

        val selected = if (isHangul(lastChar)) {
            val index = lastChar.toInt() - HANGUL_SYL_START
            if (isIrregular)
                if (index % 28 == 0 || index % 28 == 8) josa1 else josa2
            else
                if (index % 28 == 0) josa1 else josa2
        }
        else if (lastChar in 'A'..'Z' || lastChar in 'a'..'z') {
            val index = (lastChar.toInt() - 0x41) % 0x20
            if (isIrregular)
                if (HANGUL_POST_RO_INDEX_ALPH[index] == 0) josa1 else josa2
            else
                if (HANGUL_POST_INDEX_ALPH[index] == 0) josa1 else josa2
        }
        else {
            josa2
        }

        return "$word$selected"
    }

    private fun isHangul(c: Char): Boolean {
        return c.toInt() in 0xAC00..0xD7A3
    }

    private fun getLastChar(s: String): Char {
        return s[s.length - 1]
    }
}
