package net.torvald.terrarum.tvda

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object DiskSkimmerTest {

    val fullBattery = listOf(
            { invoke00() }
    )

    operator fun invoke() {
        fullBattery.forEach { it.invoke() }
    }

    /**
     * Testing of DiskSkimmer
     */
    fun invoke00() {
        val _infile = File("./test-assets/tevd-test-suite-00.tevd")
        val outfile = File("./test-assets/tevd-test-suite-00_results.tevd")

        Files.copy(_infile.toPath(), outfile.toPath(), StandardCopyOption.REPLACE_EXISTING)

/*
Copied from instruction.txt

1. Create a file named "World!.txt" in the root directory.
2. Append "This is not SimCity 3k" on the file ./01_preamble/append-after-me
3. Delete a file ./01_preamble/deleteme
4. Modify this very file, delete everything and simply replace with "Mischief Managed."
5. Read the file ./instruction.txt and print its contents.

Expected console output:

Mischief Managed.
 */
        val skimmer = DiskSkimmer(outfile)


        println("=============================")


    }
}

fun main(args: Array<String>) {
    DiskSkimmerTest()
}