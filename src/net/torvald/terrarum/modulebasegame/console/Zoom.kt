package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2016-01-25.
 */
internal object Zoom : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {

            var zoom: Float
            try {
                zoom = args[1].toFloat()
            }
            catch (e: NumberFormatException) {
                Echo("Wrong number input.")
                return
            }

            if (zoom < INGAME.ZOOM_MINIMUM) {
                zoom = INGAME.ZOOM_MINIMUM
            }
            else if (zoom > INGAME.ZOOM_MAXIMUM) {
                zoom = INGAME.ZOOM_MAXIMUM
            }

            INGAME.screenZoom = zoom

            System.gc()

            Echo("Set screen zoom to " + zoom.toString())
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: zoom [zoom]")
    }
}
