package net.torvald.terrarum.console

import net.torvald.terrarum.AppLoader

object TakeScreenshot: ConsoleCommand {
    override fun execute(args: Array<String>) {
        AppLoader.requestScreenshot()
    }

    override fun printUsage() {
        Echo("Takes screenshot and save it to the default directory as 'screenshot.tga'")
    }
}