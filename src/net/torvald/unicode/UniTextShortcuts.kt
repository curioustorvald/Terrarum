package net.torvald.unicode

import net.torvald.terrarum.savegame.ByteArray64

/**
 * Created by minjaesong on 2019-08-05.
 */


const val CURRENCY = 0xA4.toChar()
const val MIDDOT = 0xB7.toChar()
const val TIMES = 0xD7.toChar()

const val ENDASH = 0x2013.toChar()
const val EMDASH = 0x2014.toChar()
const val ELLIPSIS = 0x2026.toChar()
const val BULLET = 0x2022.toChar()

const val KEYCAP_SHIFT = 0xE001.toChar()
const val KEYCAP_CAPS = 0xE002.toChar()
const val KEYCAP_TAB = 0xE003.toChar()
const val KEYCAP_DELETE = 0xE004.toChar()
const val KEYCAP_RETURN = 0xE005.toChar()
const val KEYCAP_LEFT_MOUSE = 0xE006.toChar()
const val KEYCAP_RIGHT_MOUSE = 0xE007.toChar()
const val KEYCAP_WHEEL = 0xE008.toChar()
const val KEYCAP_MOVE = 0xE009.toChar()
const val KEYCAP_CTRL = 0xE00A.toChar()
const val KEYCAP_ALT = 0xE00B.toChar()

const val KEYCAP_0 = 0xE010.toChar()
const val KEYCAP_1 = 0xE011.toChar()
const val KEYCAP_2 = 0xE012.toChar()
const val KEYCAP_3 = 0xE013.toChar()
const val KEYCAP_4 = 0xE014.toChar()
const val KEYCAP_5 = 0xE015.toChar()
const val KEYCAP_6 = 0xE016.toChar()
const val KEYCAP_7 = 0xE017.toChar()
const val KEYCAP_8 = 0xE018.toChar()
const val KEYCAP_9 = 0xE019.toChar()

const val KEYCAP_A = 0xE021.toChar()
const val KEYCAP_B = 0xE022.toChar()
const val KEYCAP_C = 0xE023.toChar()
const val KEYCAP_D = 0xE024.toChar()
const val KEYCAP_E = 0xE025.toChar()
const val KEYCAP_F = 0xE026.toChar()
const val KEYCAP_G = 0xE027.toChar()
const val KEYCAP_H = 0xE028.toChar()
const val KEYCAP_I = 0xE029.toChar()
const val KEYCAP_J = 0xE02A.toChar()
const val KEYCAP_K = 0xE02B.toChar()
const val KEYCAP_L = 0xE02C.toChar()
const val KEYCAP_M = 0xE02D.toChar()
const val KEYCAP_N = 0xE02E.toChar()
const val KEYCAP_O = 0xE02F.toChar()
const val KEYCAP_P = 0xE030.toChar()
const val KEYCAP_Q = 0xE031.toChar()
const val KEYCAP_R = 0xE032.toChar()
const val KEYCAP_S = 0xE033.toChar()
const val KEYCAP_T = 0xE034.toChar()
const val KEYCAP_U = 0xE035.toChar()
const val KEYCAP_V = 0xE036.toChar()
const val KEYCAP_W = 0xE037.toChar()
const val KEYCAP_X = 0xE038.toChar()
const val KEYCAP_Y = 0xE039.toChar()
const val KEYCAP_Z = 0xE03A.toChar()

fun getKeycapPC(c: Char) = when (c.uppercaseChar()) {
    in ' '..'_' -> (0xE000 + c.code - 32).toChar()
    else -> throw IllegalArgumentException("Not in range: ${c.code - 32}")
}
fun getKeycapPC(keycode: Int) = getKeycapPC(com.badlogic.gdx.Input.Keys.toString(keycode)[0])
fun getKeycapConsole(c: Char) = when (c.uppercaseChar()) {
    in ' '..'_' -> (0xE040 + c.code - 32).toChar()
    else -> throw IllegalArgumentException("Not in range: ${c.code - 32}")
}
fun getKeycapFkeys(n: Int) = when (n) {
    in 1..9 -> "\uE090${(0xE090 + n).toChar()}"
    in 10..12 -> "\uE09D${(0xE090 + n).toChar()}"
    else -> throw IllegalArgumentException("Not in range: $n")
}

fun List<Int>.toJavaString(): String {
    val sb = StringBuilder()
    this.forEach {
        if (it > 65535) {
            val u = it - 65536
            sb.append((0xD800 or (u ushr 10).and(1023)).toChar())
            sb.append((0xDC00 or (u and 1023)).toChar())
        }
        else {
            sb.append(it.toChar())
        }
    }
    return sb.toString()
}

fun List<Int>.toUTF8Bytes64(): ByteArray64 {
    val ba = ByteArray64()
    this.forEach { codepoint ->
        when (codepoint) {
            in 0..127 -> ba.appendByte(codepoint.toByte())
            in 128..2047 -> {
                ba.appendByte((0xC0 or codepoint.ushr(6).and(31)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            in 2048..65535 -> {
                ba.appendByte((0xE0 or codepoint.ushr(12).and(15)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            in 65536..1114111 -> {
                ba.appendByte((0xF0 or codepoint.ushr(18).and(7)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(12).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.appendByte((0x80 or codepoint.and(63)).toByte())
            }
            else -> throw IllegalArgumentException("Not a unicode code point: U+${codepoint.toString(16).toUpperCase()}")
        }
    }
    return ba
}

fun List<Int>.toUTF8Bytes() = this.toUTF8Bytes64().toByteArray()