package net.torvald.terrarum.console

import net.torvald.terrarum.modulebasegame.IngameRenderer

object ExportRendererFboRGB: ConsoleCommand {

    override fun execute(args: Array<String>) {
        IngameRenderer.fboRGBexportRequested = true
    }

    override fun printUsage() {

    }
}