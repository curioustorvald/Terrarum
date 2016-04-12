package net.torvald.terrarum.langpack

import net.torvald.CSVFetcher
import net.torvald.imagefont.GameFontWhite
import net.torvald.terrarum.Terrarum
import org.apache.commons.csv.CSVRecord
import org.newdawn.slick.SlickException

import java.io.*
import java.util.*

/**
 * Created by minjaesong on 16-01-22.
 */
object Lang {

    private val CSV_COLUMN_FIRST = "STRING_ID"
    /**
     * Get record by its STRING_ID
     */
    private var lang: HashMap<String, CSVRecord>
    private val FALLBACK_LANG_CODE = "enUS"

    private val HANGUL_SYL_START = 0xAC00

    private val PATH_TO_CSV = "./res/locales/"
    private val CSV_MAIN = "polyglot.csv"
    private val NAMESET_PREFIX = "nameset_"

    private val HANGUL_POST_INDEX_ALPH = intArrayOf(// 0: 는, 가, ...  1: 은, 이, ...
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val HANGUL_POST_RO_INDEX_ALPH = intArrayOf(// 0: 로  1: 으로
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    private val ENGLISH_WORD_NORMAL_PLURAL = arrayOf("photo")

    private val FRENCH_WORD_NORMAL_PLURAL = arrayOf("bal", "banal", "fatal", "final")

    init {
        lang = HashMap<String, CSVRecord>()

        // append CSV records to the main langpack
        val file = File(PATH_TO_CSV)
        val filter = FilenameFilter { dir, name -> name.contains(".csv") && !name.contains(NAMESET_PREFIX) }
        for (csvfilename in file.list(filter)) {
            val csv = CSVFetcher.readCSV(PATH_TO_CSV + csvfilename)
            //csv.forEach({ langPackCSV. })
            csv.forEach { it -> lang.put(it.get(CSV_COLUMN_FIRST), it) }
        }

        // sort word lists
        Arrays.sort(ENGLISH_WORD_NORMAL_PLURAL)
        Arrays.sort(FRENCH_WORD_NORMAL_PLURAL)

        // reload correct (C/J) unihan fonts if applicable
        try {
            (Terrarum.gameFont as GameFontWhite).reloadUnihan()
        }
        catch (e: SlickException) {
        }

    }

    fun getRecord(key: String): CSVRecord {
        val record = lang[key]
        if (record == null) {
            println("[Lang] No such record: $key")
            throw NullPointerException()
        }
        return record
    }

    operator fun get(key: String): String {
        fun fallback(): String = lang[key]!!.get(FALLBACK_LANG_CODE)

        var value: String
        try {
            value = lang[key]!!.get(Terrarum.gameLocale)
            // fallback if empty string
            if (value.length == 0)
                value = fallback()
        }
        catch (e1: kotlin.KotlinNullPointerException) {
            value = "ERRNULL:$key"
        }
        catch (e: IllegalArgumentException) {
            //value = key
            value = fallback()
        }

        return value
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

    private fun appendToLangByStringID(record: CSVRecord) {
        lang.put(record.get(CSV_COLUMN_FIRST), record)
    }
}
