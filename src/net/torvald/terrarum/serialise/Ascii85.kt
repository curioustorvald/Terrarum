package net.torvald.terrarum.serialise

import net.torvald.terrarum.savegame.toBigEndian
import java.util.UUID
import kotlin.math.ceil

/** My own string set that:
 * - no "/": avoids nonstandard JSON comment key which GDX will happily parse away
 * - no "\": you know what I mean\\intention
 * - no "$": avoids Kotlin string template
 * - no "[{]},": even the dumbest parser can comprehend the output
 */
open class Ascii85Codec(private val CHAR_TABLE: String = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!#%&'()*+-.:;<=>?@^_`|~") {
    init {
        if (CHAR_TABLE.length != 85) throw IllegalArgumentException("CHAR_TABLE is not 85 chars long")
    }

    /** As per Adobe standard */
    //private val CHAR_TABLE = (33 until (33+85)).toList().map { it.toChar() }.joinToString("") // testing only!

    private val INVERSE_TABLE = LongArray(127)

    /** Int of `-1` */
    val PAD_BYTE = -1
    /** Null-character (`\0`) */
    val PAD_CHAR = 0.toChar()

    private val INTERNAL_PAD_BYTE = 0
    private val INTERNAL_PAD_CHAR = CHAR_TABLE.last()

    init {
        for (i in 0 until 85)
            INVERSE_TABLE[CHAR_TABLE[i].toInt()] = i.toLong()
    }

    fun encode(i1: Int?, i2: Int?, i3: Int?, i4: Int?): String {
        var b1=i1 ?: PAD_BYTE; var b2=i2 ?: PAD_BYTE; var b3=i3 ?: PAD_BYTE; var b4=i4 ?: PAD_BYTE
        var padLen = 0
        if (b4 == PAD_BYTE) {
            b4 = INTERNAL_PAD_BYTE
            padLen = 1
        }
        if (b3 == PAD_BYTE) {
            b3 = INTERNAL_PAD_BYTE
            padLen = 2
        }
        if (b2 == PAD_BYTE) {
            b2 = INTERNAL_PAD_BYTE
            padLen = 3
        }
        if (b1 == PAD_BYTE) {
            return ""
        }

        var sum = (b1.toLong().and(255) shl 24) or
                (b2.toLong().and(255) shl 16) or
                (b3.toLong().and(255) shl 8) or
                b4.toLong().and(255)
        val c1 = (sum / 52200625L).toInt()
        sum -= (c1 * 52200625L)
        val c2 = (sum / 614125L).toInt()
        sum -= (c2 * 614125L)
        val c3 = (sum / 7225L).toInt()
        sum -= (c3 * 7225L)
        val c4 = (sum / 85L).toInt()
        sum %= 85L

        return ("${CHAR_TABLE[c1]}" +
                "${CHAR_TABLE[c2]}" +
                "${CHAR_TABLE[c3]}" +
                "${CHAR_TABLE[c4]}" +
                "${CHAR_TABLE[sum.toInt()]}").substring(0,5 - padLen)
    }

    fun decode(x1: Char?, x2: Char?, x3: Char?, x4: Char?, x5: Char?): ByteArray {
        var s1=x1 ?: PAD_CHAR; var s2=x2 ?: PAD_CHAR; var s3=x3 ?: PAD_CHAR; var s4=x4 ?: PAD_CHAR; var s5=x5 ?: PAD_CHAR
        var padLen = 0
        if (s5 == PAD_CHAR) {
            s5 = INTERNAL_PAD_CHAR
            padLen = 1
        }
        if (s4 == PAD_CHAR) {
            s4 = INTERNAL_PAD_CHAR
            padLen = 2
        }
        if (s3 == PAD_CHAR) {
            s3 = INTERNAL_PAD_CHAR
            padLen = 3
        }
        if (s2 == PAD_CHAR) {
            s2 = INTERNAL_PAD_CHAR
            padLen = 3
        }
        if (s1 == PAD_CHAR) {
            return byteArrayOf()
        }

        val sum = (INVERSE_TABLE[s1.toInt()] * 52200625) +
                (INVERSE_TABLE[s2.toInt()] * 614125) +
                (INVERSE_TABLE[s3.toInt()] * 7225) +
                (INVERSE_TABLE[s4.toInt()] * 85) +
                INVERSE_TABLE[s5.toInt()]
        return ByteArray(4 - padLen) { sum.ushr((3 - it) * 8).and(255).toByte() }
    }

    fun encodeBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (k in 0..(bytes.size / 4)) {
            sb.append(Ascii85.encode(
                bytes.getOrNull(k*4)?.toInt(),
                bytes.getOrNull(k*4+1)?.toInt(),
                bytes.getOrNull(k*4+2)?.toInt(),
                bytes.getOrNull(k*4+3)?.toInt()
            ))
        }
        return sb.toString()
    }
    fun decodeBytes(encoded: String): ByteArray {
        val bytes = ByteArray(ceil(encoded.length * 0.8).toInt())
        var curs = 0
        for (k in 0..(encoded.length / 5)) {
            Ascii85.decode(
                encoded.getOrNull(k*5),
                encoded.getOrNull(k*5+1),
                encoded.getOrNull(k*5+2),
                encoded.getOrNull(k*5+3),
                encoded.getOrNull(k*5+4),
            ).let {
                it.forEachIndexed { i, b ->
                    bytes[curs + i] = b
                }
                curs += it.size
            }
        }
        return bytes
    }
}

/**
 * Ascii85 implementation with my own character table based on RFC 1924. Will NOT truncate '00000' into something else;
 * just gzip the inputstream instead!
 */
object Ascii85 : Ascii85Codec()

fun UUID.toAscii85() =
    Ascii85.encodeBytes(this.mostSignificantBits.toBigEndian() + this.leastSignificantBits.toBigEndian())
fun String.ascii85toUUID(): UUID {
    val bytes = Ascii85.decodeBytes(this)
    val msb = bytes.toBigInt64(0)
    val lsb = bytes.toBigInt64(8)
    return UUID(msb, lsb)
}

/*fun main(args: Array<String>) {
    val str = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure."
    println(str)

    var out = ""
    for (k in 0..(str.length / 4)) {
        out += Ascii85.encode(
                str.getOrNull(k*4)?.toInt(),
                str.getOrNull(k*4+1)?.toInt(),
                str.getOrNull(k*4+2)?.toInt(),
                str.getOrNull(k*4+3)?.toInt()
        )
    }

    println(out)

    ////////////////////////////////

    val encoded = out

    var out2 = ""
    for (k in 0..(encoded.length / 5)) {
        out2 += Ascii85.decode(
                encoded.getOrNull(k*5),
                encoded.getOrNull(k*5+1),
                encoded.getOrNull(k*5+2),
                encoded.getOrNull(k*5+3),
                encoded.getOrNull(k*5+4),
        ).toString(Charsets.US_ASCII)
    }

    println(out2)
}*/