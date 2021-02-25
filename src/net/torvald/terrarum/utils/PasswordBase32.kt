package net.torvald.terrarum.utils

import kotlin.experimental.xor


/**
 * Old-school passworld system using Base32
 *
 * Created by minjaesong on 2017-05-02.
 */
object PasswordBase32 {

    private val stringSet = "YBNDRFG8EJKMCPQXOTLVWIS2A345H769="

    private val substituteSet = hashMapOf(
            Pair('0', 'O'),
            Pair('1', 'I'),
            Pair('Z', '2')
    )

    /*
    0   x
    1   6
    2   4
    3   3
    4   1
     */
    val padLen = arrayOf(0, 6, 4, 3, 1)
    private val nullPw = byteArrayOf(0.toByte())


    private fun encodeToLetters(byteArray: ByteArray, password: ByteArray): IntArray {
        val out = ArrayList<Int>()

        fun get(i: Int) = byteArray[i] xor (password[i % password.size])


        /*
        5 Bytes -> 8 Letters

        0000 0000 | 1111 1111 | 2222 2222 | 3333 3333 | 4444 4444
        AAAA ABBB | BBCC CCCD | DDDD EEEE | EFFF FFGG | GGGH HHHH
         */

        // non-pads
        (0..byteArray.lastIndex - 5 step 5).forEach {
            /* A */ out.add(get(it).toInt().and(0xF8).ushr(3))
            /* B */ out.add(get(it).toInt().and(7).shl(2) or get(it+1).toInt().and(0xC0).ushr(6))
            /* C */ out.add(get(it+1).toInt().and(0x3E).ushr(1))
            /* D */ out.add(get(it+1).toInt().and(1).shl(4) or get(it+2).toInt().and(0xF0).ushr(4))
            /* E */ out.add(get(it+2).toInt().and(0xF).shl(1) or get(it+3).toInt().and(0x80).ushr(7))
            /* F */ out.add(get(it+3).toInt().and(0x7C).ushr(2))
            /* G */ out.add(get(it+3).toInt().and(3).shl(3) or get(it+4).toInt().and(0xE0).ushr(5))
            /* H */ out.add(get(it+4).toInt().and(0x1F))
        }
        // pads
        val residue = byteArray.size % 5
        if (residue != 0){

            val it = (byteArray.size / 5) * 5 // dark magic of integer division, let's hope the compiler won't "optimise" this...

            when (residue) {
                1 -> {
                    /* A */ out.add(get(it).toInt().and(0xF8).ushr(3))
                    /* B */ out.add(get(it).toInt().and(7).shl(2))
                }
                2 -> {
                    /* A */ out.add(get(it).toInt().and(0xF8).ushr(3))
                    /* B */ out.add(get(it).toInt().and(7).shl(2) or get(it+1).toInt().and(0xC0).ushr(6))
                    /* C */ out.add(get(it+1).toInt().and(0x3E).ushr(1))
                    /* D */ out.add(get(it+1).toInt().and(1).shl(4))
                }
                3 -> {
                    /* A */ out.add(get(it).toInt().and(0xF8).ushr(3))
                    /* B */ out.add(get(it).toInt().and(7).shl(2) or get(it+1).toInt().and(0xC0).ushr(6))
                    /* C */ out.add(get(it+1).toInt().and(0x3E).ushr(1))
                    /* D */ out.add(get(it+1).toInt().and(1).shl(4) or get(it+2).toInt().and(0xF0).ushr(4))
                    /* E */ out.add(get(it+2).toInt().and(0xF).shl(1))
                }
                4 -> {
                    /* A */ out.add(get(it).toInt().and(0xF8).ushr(3))
                    /* B */ out.add(get(it).toInt().and(7).shl(2) or get(it+1).toInt().and(0xC0).ushr(6))
                    /* C */ out.add(get(it+1).toInt().and(0x3E).ushr(1))
                    /* D */ out.add(get(it+1).toInt().and(1).shl(4) or get(it+2).toInt().and(0xF0).ushr(4))
                    /* E */ out.add(get(it+2).toInt().and(0xF).shl(1) or get(it+3).toInt().and(0x80).ushr(7))
                    /* F */ out.add(get(it+3).toInt().and(0x7C).ushr(2))
                    /* G */ out.add(get(it+3).toInt().and(3).shl(3))
                }
            }

            // append padding
            kotlin.repeat(padLen[residue], { out.add(32) })
        }

        return out.toIntArray()
    }

    /**
     *
     * @param bytes size of multiple of five (5, 10, 15, 20, ...) is highly recommended to prevent
     * lengthy padding
     * @param password to encode resulting string using XOR Cipher to prevent unexperienced kids
     * from doing naughty things. Longer, the better.
     */
    fun encode(bytes: ByteArray, password: ByteArray = nullPw): String {
        val plaintext = encodeToLetters(bytes, password)
        val sb = StringBuilder()
        plaintext.forEach { sb.append(stringSet[it]) }

        return sb.toString()
    }

    /**
     * @param input password input from the user. Will be automatically converted to uppercase and
     * will correct common mistakes.
     * @param outByteLength expected length of your decoded password. It is always a good idea to
     * suspect user inputs and sanitise them.
     */
    fun decode(input: String, outByteLength: Int, password: ByteArray = nullPw): ByteArray {
        val buffer = ByteArray(outByteLength)
        var appendCount = 0
        var input = input.toUpperCase()
        substituteSet.forEach { from, to ->
            input = input.replace(from, to)
        }

        fun append(byte: Int) {
            buffer[appendCount] = byte.toByte() xor (password[appendCount % password.size])
            appendCount++
        }
        fun sbyteOf(i: Int) = stringSet.indexOf(input[i]).and(0x1F)

        try {
            /*
            8 Letters -> 5 Bytes

            0000 0000 | 1111 1111 | 2222 2222 | 3333 3333 | 4444 4444
            AAAA ABBB | BBCC CCCD | DDDD EEEE | EFFF FFGG | GGGH HHHH
             */
            (0..input.lastIndex.plus(8) step 8).forEach {
                /* 0 */ append(sbyteOf(it+0).shl(3) or sbyteOf(it+1).ushr(2))
                /* 1 */ append(sbyteOf(it+1).shl(6) or sbyteOf(it+2).shl(1) or sbyteOf(it+3).ushr(4))
                /* 2 */ append(sbyteOf(it+3).shl(4) or sbyteOf(it+4).ushr(1))
                /* 3 */ append(sbyteOf(it+4).shl(7) or sbyteOf(it+5).shl(2) or sbyteOf(it+6).ushr(3))
                /* 4 */ append(sbyteOf(it+6).shl(5) or sbyteOf(it+7))
            }
        }
        catch (endOfStream: ArrayIndexOutOfBoundsException) { }

        return buffer
    }
}
