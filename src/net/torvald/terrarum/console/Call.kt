package net.torvald.terrarum.console

import net.torvald.terrarum.App
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Created by minjaesong on 2016-03-07.
 */
internal object Call : ConsoleCommand {
    @Throws(Exception::class)
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            Files.lines(Path(App.defaultDir, args[1])).forEach(
                    { CommandInterpreter.execute(it) })
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: batch path/to/batch.txt relative to the app data folder")
    }
}
