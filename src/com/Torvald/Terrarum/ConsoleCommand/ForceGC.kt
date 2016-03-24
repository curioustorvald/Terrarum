package com.Torvald.Terrarum.ConsoleCommand

/**
 * Created by minjaesong on 16-01-18.
 */
class ForceGC : ConsoleCommand {
    override fun execute(args: Array<String>) {
        System.gc()
        Echo().execute("Invoked System.gc")
    }

    override fun printUsage() {
        Echo().execute("Invoke garbage collection of JVM.")
    }
}
