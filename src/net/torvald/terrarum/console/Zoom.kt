package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX

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

            if (zoom < TerrarumGDX.ingame!!.ZOOM_MIN) {
                zoom = TerrarumGDX.ingame!!.ZOOM_MIN
            }
            else if (zoom > TerrarumGDX.ingame!!.ZOOM_MAX) {
                zoom = TerrarumGDX.ingame!!.ZOOM_MAX
            }

            TerrarumGDX.ingame!!.screenZoom = zoom

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
