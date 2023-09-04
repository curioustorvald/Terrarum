package net.torvald.terrarum.tests

import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.utils.PasswordBase32
import java.nio.charset.Charset
import java.util.*

object Base32Test {

    operator fun invoke() {
        val testStr = UUID.fromString("145efab2-d465-4e1e-abae-db6c809817a9").let {
            it.mostSignificantBits.toBig64() + it.leastSignificantBits.toBig64()
        }
        val pwd = "béchamel".toByteArray()

        val enc = PasswordBase32.encode(testStr, pwd)
        val dec = PasswordBase32.decode(enc, testStr.size, pwd)

        println("Encoded text: $enc")
        println("Encoded bytes: ${testStr.joinToString(" ") { it.toInt().and(255).toString(16).padStart(2, '0') }}")
        println("Decoded bytes: ${dec.joinToString(" ") { it.toInt().and(255).toString(16).padStart(2, '0') }}")
    }

}

fun main(args: Array<String>) {
    Base32Test.invoke()
}