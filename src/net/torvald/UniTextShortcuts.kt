package net.torvald

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
            in 0..127 -> ba.add(codepoint.toByte())
            in 128..2047 -> {
                ba.add((0xC0 or codepoint.ushr(6).and(31)).toByte())
                ba.add((0x80 or codepoint.and(63)).toByte())
            }
            in 2048..65535 -> {
                ba.add((0xE0 or codepoint.ushr(12).and(15)).toByte())
                ba.add((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.add((0x80 or codepoint.and(63)).toByte())
            }
            in 65536..1114111 -> {
                ba.add((0xF0 or codepoint.ushr(18).and(7)).toByte())
                ba.add((0x80 or codepoint.ushr(12).and(63)).toByte())
                ba.add((0x80 or codepoint.ushr(6).and(63)).toByte())
                ba.add((0x80 or codepoint.and(63)).toByte())
            }
            else -> throw IllegalArgumentException("Not a unicode code point: U+${codepoint.toString(16).toUpperCase()}")
        }
    }
    return ba
}

fun List<Int>.toUTF8Bytes() = this.toUTF8Bytes64().toByteArray()