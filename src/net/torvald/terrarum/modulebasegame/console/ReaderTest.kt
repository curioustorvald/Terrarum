package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.serialise.ByteArray64Reader
import net.torvald.terrarum.serialise.ByteArray64Writer
import java.io.File

/**
 * Created by minjaesong on 2021-08-31.
 */
object ReaderTest : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val textfile = File("./work_files/utftest.txt")
        val text = textfile.readText()

        val writer = ByteArray64Writer(Charsets.UTF_8)
        writer.write(text); writer.flush(); writer.close()

        val ba = writer.toByteArray64()

        val reader = ByteArray64Reader(ba, Charsets.UTF_8)
        val readText = reader.readText(); reader.close()

        println(readText)
        val outfile = File("./work_files/utftest-roundtrip.txt")
        outfile.writeText(readText, Charsets.UTF_8)
    }

    override fun printUsage() {
        Echo("Usage: readertest")
    }
}