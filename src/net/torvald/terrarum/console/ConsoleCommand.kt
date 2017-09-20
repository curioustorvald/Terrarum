package net.torvald.terrarum.console

/**
 * Created by minjaesong on 2016-01-15.
 */
interface ConsoleCommand {

    /**
     * Args 0: command given
     * Args 1: first argument
     *
     * e.g. in ```setav mass 74```, zeroth args will be ```setav```.
     */
    fun execute(args: Array<String>)

    fun printUsage()

}