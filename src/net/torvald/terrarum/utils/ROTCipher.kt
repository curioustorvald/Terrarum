package net.torvald.terrarum.utils

/**
 * Created by minjaesong on 2016-03-20.
 */
object ROTCipher {

    const val CODE_CAP_A = 'A'.toInt()
    const val CODE_LOW_A = 'a'.toInt()

    private val substituteSet = hashMapOf(
            Pair('À', "A"),
            Pair('Á', "A"),
            Pair('Â', "A"),
            Pair('À', "A"),
            Pair('Ã', "A"),
            Pair('Å', "A"),
            Pair('Æ', "AE"),
            Pair('Ç', "C"),
            Pair('È', "E"),
            Pair('É', "E"),
            Pair('Ê', "E"),
            Pair('Ë', "E"),
            Pair('Ì', "I"),
            Pair('Í', "I"),
            Pair('Î', "I"),
            Pair('Ï', "I"),
            Pair('Ð', "D"),
            Pair('Ñ', "N"),
            Pair('Ò', "O"),
            Pair('Ó', "O"),
            Pair('Ô', "O"),
            Pair('Õ', "O"),
            Pair('Ö', "OE"),
            Pair('Ø', "OE"),
            Pair('Ù', "U"),
            Pair('Ú', "U"),
            Pair('Û', "U"),
            Pair('Ü', "Y"),
            Pair('Ý', "TH"),
            Pair('Þ', "TH"),
            Pair('ß', "SS")
    )

    /**
     * ROT encryption, default setting
     *
     * * Latin alph: removes diacritics and do ROT13, string reverse, capitalised.
     * Ligatures are disassembled. Even if the target language does not have
     * certain alphabet (e.g. C in Icelandic), such alphabet will be printed anyway.
     * * Numeric: no encrypt
     */
    fun encrypt(plaintext: String): String {
        var plaintext = plaintext.toUpperCase()
        substituteSet.forEach { from, to ->
            plaintext = plaintext.replace(from.toString(), to)
        }
        plaintext = plaintext.reversed()

        val sb = StringBuilder()
        plaintext.forEach { sb.append(rot13(it)) }

        return sb.toString()
    }

    /**
     * Note; domain starts from zero
     * @param number to rotate
     * @param rotation
     * @param domain size of the rotation table (or domain of the function). 4 means (0,1,2,3)
     */
    private fun rotN(number: Int, rotation: Int, domain: Int): Int {
        return if (number < domain - rotation + 1) number + rotation
        else      number - (domain - rotation + 1)
    }

    fun rot13(c: Char): Char {
        return if (c in 'a'..'z')
            (rotN((c.toInt() - CODE_LOW_A), 13, 26) + CODE_LOW_A).toChar()
        else if (c in 'A'..'Z')
            (rotN((c.toInt() - CODE_CAP_A), 13, 26) + CODE_CAP_A).toChar()
        else c
    }
}