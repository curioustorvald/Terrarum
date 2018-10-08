package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.serialise.WriteLayerDataLzma
import net.torvald.terrarum.serialise.WriteLayerDataZip
import net.torvald.terrarum.serialise.WriteWorldInfo

/**
 * Created by minjaesong on 2017-07-18.
 */
object ExportLayerData : ConsoleCommand {
    override fun execute(args: Array<String>) {
        try {
            val outfile = WriteLayerDataZip()
            WriteWorldInfo()
            Echo("Layer data exported to ${outfile!!.canonicalPath}")
        }
        catch (e: Exception) {
            e.printStackTrace()
            EchoError("Layer data export failed; see console for error traces.")
        }
    }

    override fun printUsage() {
        Echo("Usage: exportlayer")
    }
}