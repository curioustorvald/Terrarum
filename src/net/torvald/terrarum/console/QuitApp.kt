package net.torvald.terrarum.console

/**
 * Created by minjaesong on 2016-01-15.
 */
internal object QuitApp : ConsoleCommand {

    override fun execute(args: Array<String>) {
        System.exit(1)
    }

    override fun printUsage() {

    }
}
