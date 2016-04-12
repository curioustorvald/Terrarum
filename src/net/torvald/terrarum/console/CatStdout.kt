package net.torvald.terrarum.console

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files

/**
 * Created by minjaesong on 16-02-10.
 */
class CatStdout : ConsoleCommand {
    override fun execute(args: Array<String>) {

        val echo = Echo()

        if (args.size == 1) {
            printUsage()
            return
        }

        try {
            Files.lines(FileSystems.getDefault().getPath(args[1])).forEach({ echo.execute(it) })
        }
        catch (e: IOException) {
            echo.execute("CatStdout: could not read file -- IOException")
        }

    }

    override fun printUsage() {
        Echo().execute("usage: cat 'path/to/text/file")
    }
}
