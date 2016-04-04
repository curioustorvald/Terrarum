package com.torvald.terrarum.console

import com.torvald.terrarum.mapdrawer.LightmapRenderer
import com.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-02-17.
 */
class SetGlobalLightLevel : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 4) {
            try {
                val r = args[1].toInt()
                val g = args[2].toInt()
                val b = args[3].toInt()
                val GL = LightmapRenderer.constructRGBFromInt(r, g, b)

                Terrarum.game.map.globalLight = GL
            }
            catch (e: NumberFormatException) {
                Echo().execute("Wrong number input.")
            }
            catch (e1: IllegalArgumentException) {
                Echo().execute("Range: 0-" + LightmapRenderer.CHANNEL_MAX + " per channel")
            }

        }
        else if (args.size == 2) {
            try {
                val GL = args[1].toInt()

                if (GL.toInt() < 0 || GL.toInt() >= LightmapRenderer.COLOUR_RANGE_SIZE) {
                    Echo().execute("Range: 0-" + (LightmapRenderer.COLOUR_RANGE_SIZE - 1))
                }
                else {
                    Terrarum.game.map.globalLight = GL
                }
            }
            catch (e: NumberFormatException) {
                Echo().execute("Wrong number input.")
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("Usage: setgl [raw_value|r g b]")
    }
}
