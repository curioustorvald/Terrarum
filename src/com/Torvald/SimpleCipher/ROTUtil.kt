package com.torvald.simplecipher

/**
 * Created by minjaesong on 16-03-20.
 */
object ROTUtil {

    const val CODE_CAP_A = 'A'.toInt()
    const val CODE_LOW_A = 'a'.toInt()

    /**
     * ROT encryption, default setting
     *
     * * Latin alph: removes diacritics and do ROT13, string reverse, capitalised.
     * Ligatures are disassembled. Even if the target language does not have
     * certain alphabet (e.g. C in Icelandic), such alphabet will be printed anyway.
     *
     * * Cyrillic: removes diacritics and do ROTnn, string reverse, capitalised.
     *
     * * Kana: raise Sutegana, ROT3 on vowels (a i u e o -> e o a i u), string reverse
     * (Wa Wo -> Wo Wa), Nn will remain untouched, forced Katakana.
     *
     * * Hangul: ROT3 on initials\*, lower double initials to single
     * (ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ -> ㄹ ㄹ ㅁ ㅂ ㅂ ㅅ ㅇ ㅇ ㅈ ㅈ ㅊ ㅋ ㅋ ㅌ ㅍ ㅎ ㄱ ㄴ ㄷ),
     * string reverse
     *
     * * Chinese: NOT SUPPORTED
     *
     * * Numeric: no encrypt
     */
    fun encrypt(plaintext: String): String {

        fun removeDiacritics(c: Char): Char {
            val normaliseMap = hashMapOf(
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
                    Pair('Ð', "D")
            )
            throw NotImplementedError("Feature WIP")
        }

        throw NotImplementedError("Feature WIP")
    }

    /**
     * Note; range starts with zero
     * @param number to rotate
     * @param rotation
     * @param range size of the rotation table. 4 means (0,1,2,3)
     */
    fun rotN(number: Int, rotation: Int, range: Int): Int {
        return if (number < range - rotation + 1) number + rotation
        else      number - (range - rotation + 1)
    }

    fun rot13(c: Char): Char {
        return if (c in 'a'..'z')
            (rotN((c.toInt() - CODE_LOW_A), 13, 26) + CODE_LOW_A).toChar()
        else if (c in 'A'..'Z')
            (rotN((c.toInt() - CODE_CAP_A), 13, 26) + CODE_CAP_A).toChar()
        else c
    }
}