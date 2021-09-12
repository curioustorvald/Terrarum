package net.torvald

/**
 * Created by minjaesong on 2019-08-05.
 */


const val CURRENCY = 0xA4.toChar()
const val MIDDOT = 0xB7.toChar()

const val ENDASH = 0x2013.toChar()
const val EMDASH = 0x2014.toChar()
const val ELLIPSIS = 0x2026.toChar()

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