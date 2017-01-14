package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.DecodeTapestry
import java.io.File
import java.io.FileInputStream

/**
 * Created by SKYHi14 on 2017-01-14.
 */
object SpawnTapestry : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            printUsage()
            return
        }

        val tapestry = DecodeTapestry(File(args[1]))
        Terrarum.ingame.addActor(tapestry)
    }

    override fun printUsage() {
        println("Usage: spawntapestry <tapestry_file>")
    }
}