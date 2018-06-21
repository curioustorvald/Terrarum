package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2016-01-18.
 */
internal object ForceGC : ConsoleCommand {
    override fun execute(args: Array<String>) {
        System.gc()
        Echo("Invoked System.gc")
    }

    override fun printUsage() {
        Echo("Invoke garbage collection of JVM.")
    }
}
