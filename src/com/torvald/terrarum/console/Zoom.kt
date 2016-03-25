package com.torvald.terrarum.console

import com.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-25.
 */
class Zoom : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {

            var zoom: Float
            try {
                zoom = args[1].toFloat()
            }
            catch (e: NumberFormatException) {
                Echo().execute("Wrong number input.")
                return
            }

            if (zoom < Terrarum.game.ZOOM_MIN) {
                zoom = Terrarum.game.ZOOM_MIN
            }
            else if (zoom > Terrarum.game.ZOOM_MAX) {
                zoom = Terrarum.game.ZOOM_MAX
            }

            Terrarum.game.screenZoom = zoom

            System.gc()

            Echo().execute("Set screen zoom to " + zoom.toString())
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("Usage: zoom [zoom]")
    }
}
