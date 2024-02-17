package net.torvald.terrarum.langpack

import java.util.*

/**
 * Created by minjaesong on 2024-02-17.
 */
object TitlecaseConverter {

    /**
     * @param localecode int the form of "en", "de" or "daDK" or something
     */
    private fun getJavaLocaleFromTerrarumLocaleCode(localecode: String): Locale {
        val localecode = localecode.substring(0 until minOf(4, localecode.length))
        val lang = localecode.substring(0..1)
        val country = if (localecode.length == 4) localecode.substring(2..3) else null
        return if (country == null) Locale(lang) else Locale(lang, country)
    }

    private val locEN = getJavaLocaleFromTerrarumLocaleCode("en")

    operator fun invoke(s: String, localeCode: String): String {
        return if (localeCode.startsWith("en")) titlecaseEn(s)
        else if (
            localeCode.startsWith("ja") ||
            localeCode.startsWith("ko") ||
            localeCode.startsWith("th") ||
            localeCode.startsWith("zh")
            ) passThru(s)
        else titlecaseGeneric(s, getJavaLocaleFromTerrarumLocaleCode(localeCode))
    }

    private fun passThru(s: String) = s

    private fun titlecaseGeneric(s: String, loc: Locale) = s.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(loc)
        else it.toString()
    }

    private val englishWordsNoCapital = hashSetOf(
        "a", "an",
        "the", "to", "but", "for", "or", "and", "nor", "as",
        "amid", "mid", "anti", "at", "atop", "by", "but", "come", "in", "including", "into", "less", "like", "near",
        "next", "of", "off", "on", "onto", "out", "over", "past", "per", "plus", "post", "pre", "pro",
        "re", "sans", "sub", "than", "till", "until", "under", "up", "upon", "via", "with", "within", "without",
        "except"
    )

    private fun titlecaseEn(s: String): String {
        val ssplit = s.split(' ')
        return ssplit.mapIndexed { index, it ->
            if (index == 0 || index == ssplit.lastIndex) it.capitalise(locEN)
            else if (it[0].isUpperCase()) it
            else {
                if (englishWordsNoCapital.contains(it)) it else it.capitalise(locEN)
            }
        }.joinToString(" ")
    }

    private fun String.capitalise(loc: Locale) = this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(loc)
        else it.toString()
    }

}