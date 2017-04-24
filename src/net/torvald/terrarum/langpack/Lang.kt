package net.torvald.terrarum.langpack

import net.torvald.JsonFetcher
import net.torvald.terrarum.Terrarum
import java.io.*
import java.util.*

/**
 * Created by minjaesong on 16-01-22.
 */
object Lang {

    /**
     * Get record by its STRING_ID
     *
     * HashMap<"$key_$language", Value>
     */
    var langpack: HashMap<String, String>
        private set
    private val FALLBACK_LANG_CODE = "en"

    private val HANGUL_SYL_START = 0xAC00

    val languageList: List<String>

    private val PATH_TO_LANG = "./assets/locales/"
    val POLYGLOT_VERSION = "100"
    private val PREFIX_POLYGLOT = "Polyglot-${POLYGLOT_VERSION}_"
    private val PREFIX_NAMESET = "nameset_"

    private val HANGUL_POST_INDEX_ALPH = intArrayOf(// 0: 는, 가, ...  1: 은, 이, ...
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val HANGUL_POST_RO_INDEX_ALPH = intArrayOf(// 0: 로  1: 으로
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    private val ENGLISH_WORD_NORMAL_PLURAL = arrayOf("photo", "demo")

    private val FRENCH_WORD_NORMAL_PLURAL = arrayOf("bal", "banal", "fatal", "final")

    var TIPS_COUNT = 0
        private set

    init {
        langpack = HashMap<String, String>()
        val localesDir = File(PATH_TO_LANG)

        // get all of the languages installed
        languageList = localesDir.listFiles().filter { it.isDirectory }.map { it.name }

        for (lang in languageList) {
            // load polyglot first
            val polyglotFile = File("$PATH_TO_LANG$lang/$PREFIX_POLYGLOT$lang.json")
            val json = JsonFetcher(polyglotFile)
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
            json.getAsJsonObject("resources").getAsJsonArray("data").forEach {
                langpack.put(
                        "${it.asJsonObject["n"].asString}_$lang",
                        it.asJsonObject["s"].asString
                )
            }

            // and then the rest of the lang file
            val langFileList = ArrayList<File>()

            // --> filter out files to retrieve a list of valid lang files
            val langFileListFiles = File("$PATH_TO_LANG$lang/").listFiles()
            langFileListFiles.forEach {
                if (!it.name.startsWith("Polyglot") && it.name.endsWith(".json"))
                    langFileList.add(it)
            }

            // --> put json entries in langpack
            for (langFile in langFileList) {
                val json = JsonFetcher(langFile)
                /*
                 * Terrarum langpack JSON structure is:
                 *
                 * (root object)
                 *      "<<STRING ID>>" = "<<LOCALISED TEXT>>"
                 */
                //println(json.entrySet())
                json.entrySet().forEach {
                    langpack.put("${it.key}_$lang", it.value.asString)

                    // count up TIPS_COUNT
                    if (lang == "en" && it.key.startsWith("GAME_TIPS_")) TIPS_COUNT++
                }
            }
        }
    }

    operator fun get(key: String): String {
        fun fallback(): String = langpack["${key}_$FALLBACK_LANG_CODE"] ?: "$$key"


        val ret = langpack["${key}_${Terrarum.gameLocale}"]
        val ret2 = if (ret.isNullOrEmpty()) fallback() else ret!!

        // special treatment
        if (key.startsWith("MENU_LABEL_PRESS_START_SYMBOL"))
            return ret2.replace('>', Terrarum.joypadLabelStart).capitalize()

        return ret2.capitalize()
    }

    fun pluraliseLang(key: String, count: Int): String {
        return if (count > 1) get(key + "_PLURAL") else get(key)
    }

    fun pluralise(word: String, count: Int): String {
        if (count < 2) return word

        when (Terrarum.gameLocale) {
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

    fun postEunNeun(word: String): String {
        val lastChar = getLastChar(word)

        if (isHangul(lastChar)) {
            val index = lastChar.toInt() - HANGUL_SYL_START
            return if (index % 28 == 0) word + "는" else word + "은"
        }
        else if (lastChar >= 'A' && lastChar <= 'Z' || lastChar >= 'a' && lastChar <= 'z') {
            val index = (lastChar.toInt() - 0x41) % 0x20
            return if (HANGUL_POST_INDEX_ALPH[index] == 0) word + "는" else word + "은"
        }
        else {
            return "은(는)"
        }
    }

    fun postIiGa(word: String): String {
        val lastChar = getLastChar(word)

        if (isHangul(lastChar)) {
            val index = lastChar.toInt() - HANGUL_SYL_START
            return if (index % 28 == 0) word + "가" else word + "이"
        }
        else if (lastChar >= 'A' && lastChar <= 'Z' || lastChar >= 'a' && lastChar <= 'z') {
            val index = (lastChar.toInt() - 0x41) % 0x20
            return if (HANGUL_POST_INDEX_ALPH[index] == 0) word + "가" else word + "이"
        }
        else {
            return "이(가)"
        }
    }

    private fun isHangul(c: Char): Boolean {
        return c.toInt() >= 0xAC00 && c.toInt() <= 0xD7A3
    }

    private fun getLastChar(s: String): Char {
        return s[s.length - 1]
    }
}
