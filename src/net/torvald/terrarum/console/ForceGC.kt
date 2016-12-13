package net.torvald.terrarum.console

/**
 * Created by minjaesong on 16-01-18.
 */
internal object ForceGC : ConsoleCommand {
    override fun execute(args: Array<String>) {
        System.gc()
        Echo.execute("Invoked System.gc")
    }

    override fun printUsage() {
        Echo.execute("Invoke garbage collection of JVM.")
    }
}
