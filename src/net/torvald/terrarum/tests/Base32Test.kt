import net.torvald.terrarum.utils.PasswordBase32
import java.nio.charset.Charset

object Base32Test {

    operator fun invoke() {
        val testStr = "정 참판 양반댁 규수 혼례 치른 날. 123456709".toByteArray()
        val pwd = "béchamel".toByteArray()

        val enc = PasswordBase32.encode(testStr, pwd)
        val dec = PasswordBase32.decode(enc, testStr.size, pwd)

        println("Encoded text: $enc")
        println("Decoded text: ${dec.toString(Charset.defaultCharset())}")
    }

}

fun main(args: Array<String>) {
    Base32Test.invoke()
}