package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.gameactors.DecodeTapestry
import java.io.File

/**
 * Created by minjaesong on 2017-01-14.
 */
internal object SpawnTapestry : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            printUsage()
            return
        }

        val tapestry = DecodeTapestry(File(args[1]))
        INGAME.queueActorAddition(tapestry)
    }

    override fun printUsage() {
        Echo("Usage: spawntapestry <tapestry_file>")
    }
}