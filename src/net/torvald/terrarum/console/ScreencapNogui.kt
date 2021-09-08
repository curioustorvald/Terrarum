package net.torvald.terrarum.console

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.ccG
import net.torvald.terrarum.modulebasegame.IngameRenderer

object ScreencapNogui: ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            IngameRenderer.fboRGBexportPath = AppLoader.defaultDir + "/Exports/${args[1]}.tga"
            IngameRenderer.fboRGBexportRequested = true
            Echo("FBO exported to$ccG Exports/${args[1]}.tga")
        }
    }

    override fun printUsage() {

    }
}