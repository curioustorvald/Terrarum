package net.torvald.terrarum.console

import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-02-17.
 */
internal object SetGlobalLightOverride : ConsoleCommand {

    var lightOverride = false
        private set

    override fun execute(args: Array<String>) {
        if (args.size == 4) {
            try {
                val r = args[1].toInt()
                val g = args[2].toInt()
                val b = args[3].toInt()
                val GL = LightmapRenderer.constructRGBFromInt(r, g, b)

                lightOverride = true
                Terrarum.ingame.world.globalLight = GL
            }
            catch (e: NumberFormatException) {
                Echo("Wrong number input.")
            }
            catch (e1: IllegalArgumentException) {
                Echo("Range: 0-" + LightmapRenderer.CHANNEL_MAX + " per channel")
            }

        }
        else if (args.size == 2) {
            try {
                val GL = args[1].toInt()

                if (GL.toInt() < 0 || GL.toInt() >= LightmapRenderer.COLOUR_RANGE_SIZE) {
                    Echo("Range: 0-" + (LightmapRenderer.COLOUR_RANGE_SIZE - 1))
                }
                else {
                    Terrarum.ingame.world.globalLight = GL
                }
            }
            catch (e: NumberFormatException) {
                if (args[1].toLowerCase() == "none")
                    lightOverride = false
                else
                    Echo("Wrong number input.")
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: setgl [raw_value|r g b|“none”]")
    }
}
