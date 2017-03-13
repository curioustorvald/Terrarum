package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-25.
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

            if (zoom < Terrarum.ingame!!.ZOOM_MIN) {
                zoom = Terrarum.ingame!!.ZOOM_MIN
            }
            else if (zoom > Terrarum.ingame!!.ZOOM_MAX) {
                zoom = Terrarum.ingame!!.ZOOM_MAX
            }

            Terrarum.ingame!!.screenZoom = zoom

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
