package net.torvald.terrarum.console

import net.torvald.terrarum.App
import net.torvald.terrarum.TerrarumScreenSize

@ConsoleAlias("resize")
object ResizeScreen: ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 3) {
            App.resizeScreen(args[1].toInt(), args[2].toInt())
        }
        else if (args.size == 2) {
            when (args[1]) {
                "720p" -> App.resizeScreen(1280,720)
                "1080p" -> App.resizeScreen(1920,1080)
                "default" -> App.resizeScreen(TerrarumScreenSize.defaultW, TerrarumScreenSize.defaultH)
                else -> { printUsage(); return }
            }
        }
        else {
            printUsage(); return
        }

        Echo("Screen resized to ${App.scr.width}x${App.scr.height}")
    }

    override fun printUsage() {
        Echo("Usage: resize [width] [height]. Minimum size is ${TerrarumScreenSize.minimumW}x${TerrarumScreenSize.minimumH}")
        Echo("Reserved keywords: 720p, 1080p, default")
    }
}