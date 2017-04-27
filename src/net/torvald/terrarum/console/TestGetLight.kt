package net.torvald.terrarum.console

import net.torvald.terrarum.worlddrawer.LightmapRenderer

/**
 * Created by minjaesong on 16-09-07.
 */
internal object TestGetLight : ConsoleCommand {
    /**
     * Args 0: command given
     * Args 1: first argument
     *
     * e.g. in ```setav mass 74```, zeroth args will be ```setav```.
     */
    override fun execute(args: Array<String>) {
        val x = args[1].toInt()
        val y = args[2].toInt()
        val l = LightmapRenderer.getLightRawPos(x, y)
        EchoConsole.execute(l.toString())
    }

    override fun printUsage() {
    }
}