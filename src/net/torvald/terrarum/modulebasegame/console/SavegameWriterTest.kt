package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.serialise.SavegameWriter

/**
 * Created by minjaesong on 2019-02-22.
 */
internal object SavegameWriterTest: ConsoleCommand {

    override fun execute(args: Array<String>) {
        val r = SavegameWriter.invoke(args.getOrNull(1))
        if (!r) {
            EchoError("Saving failed")
        }
    }

    override fun printUsage() {
        Echo("savetest [optional out name}")
    }
}