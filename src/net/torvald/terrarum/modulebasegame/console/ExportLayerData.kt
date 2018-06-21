package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.serialise.WriteLayerData

/**
 * Created by minjaesong on 2017-07-18.
 */
object ExportLayerData : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            printUsage()
            return
        }

        val saveDirectoryName = args[1]

        WriteLayerData(saveDirectoryName)

        Echo("Layer data exported to $saveDirectoryName/${WriteLayerData.META_FILENAME}")
    }

    override fun printUsage() {
        Echo("Usage: exportlayer savename")
    }
}