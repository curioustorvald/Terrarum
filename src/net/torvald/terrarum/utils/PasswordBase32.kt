package net.torvald.terrarum.utils

import org.apache.commons.codec.binary.Base32
import kotlin.experimental.xor


/**
 * Old-school passworld system using Base32
 *
 * Created by minjaesong on 2017-05-02.
 */
object PasswordBase32 {

    private val si = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567="
    private val so = "YBNDRFG8EJKMCPQXOTLVUIS2A345H769 "

    private val standardToModified = HashMap<Char, Char>(32)
    private val modifiedToStandard = HashMap<Char, Char>(32)

    init {
        (0 until si.length).forEach {
            standardToModified[si[it]] = so[it]
            modifiedToStandard[so[it]] = si[it]
        }

        modifiedToStandard['0'] = modifiedToStandard['O']!!
        modifiedToStandard['1'] = modifiedToStandard['I']!!
        modifiedToStandard['Z'] = modifiedToStandard['2']!!
    }

    private val nullPw = byteArrayOf(0.toByte())

    /**
     *
     * @param bytes size of multiple of five (5, 10, 15, 20, ...) is highly recommended to prevent
     * lengthy padding
     * @param password to encode resulting string using XOR Cipher to prevent unexperienced kids
     * from doing naughty things. Longer, the better.
     */
    fun encode(bytes: ByteArray, password: ByteArray = nullPw): String {
        val rawstr = Base32().encode(ByteArray(bytes.size) { bytes[it] xor password[it % password.size] }).toString(Charsets.US_ASCII)
        return rawstr.map { standardToModified[it] }.joinToString("").trim()
    }

    /**
     * @param input password input from the user. Will be automatically converted to uppercase and
     * will correct common mistakes.
     * @param outByteLength expected length of your decoded password. It is always a good idea to
     * suspect user inputs and sanitise them.
     */
    fun decode(input: String, outByteLength: Int, password: ByteArray = nullPw): ByteArray {
        val decInput = input.trim().map { modifiedToStandard[it] }.joinToString("")
        return Base32().decode(decInput).let { ba ->
            ByteArray(outByteLength) { ba.getOrElse(it) { 0.toByte() } xor password[it % password.size] }
        }
    }

}
