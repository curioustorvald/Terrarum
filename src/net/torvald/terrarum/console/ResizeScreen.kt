package net.torvald.terrarum.console

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.TerrarumScreenSize

object ResizeScreen: ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 3) {
            AppLoader.resizeScreen(args[1].toInt(), args[2].toInt())
        }
        else if (args.size == 2) {
            when (args[1]) {
                "720p" -> AppLoader.resizeScreen(1280,720)
                "1080p" -> AppLoader.resizeScreen(1920,1080)
                "default" -> AppLoader.resizeScreen(TerrarumScreenSize.defaultW, TerrarumScreenSize.defaultH)
                else -> { printUsage(); return }
            }
        }
        else {
            printUsage(); return
        }

        Echo("Screen resized to ${AppLoader.screenSize.screenW}x${AppLoader.screenSize.screenH}")
    }

    override fun printUsage() {
        Echo("Usage: resize [width] [height]. Minimum size is ${TerrarumScreenSize.minimumW}x${TerrarumScreenSize.minimumH}")
        Echo("Reserved keywords: 720p, 1080p, default")
    }
}