package com.Torvald.Terrarum.ConsoleCommand

/**
 * Created by minjaesong on 16-03-22.
 */
class Help : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo().execute(arrayOf(
                "echo",
                "Utility keys:",
                "F3: Basic debug information",
                "F7: Toggle lightmap blending",
                "F8: Toggle smooth lighting"
        ))
    }

    override fun printUsage() {
        Echo().execute("Prints some utility functions assigned to function row of the keyboard.")
    }
}