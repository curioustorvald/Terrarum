package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.Cvec
import net.torvald.terrarum.worlddrawer.LightmapRenderer

/**
 * Created by minjaesong on 2016-02-17.
 */
internal object SetGlobalLightOverride : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 5) {
            try {
                val GL = Cvec(
                        args[1].toFloat(),
                        args[2].toFloat(),
                        args[3].toFloat(),
                        args[4].toFloat()
                )

                WeatherMixer.globalLightOverridden = true
                (Terrarum.ingame!!.world).globalLight = GL
            }
            catch (e: NumberFormatException) {
                Echo("Wrong number input.")
            }
            catch (e1: IllegalArgumentException) {
                Echo("Range: 0-" + LightmapRenderer.CHANNEL_MAX + " per channel")
            }

        }
        else if (args.size == 2 && args[1].trim().toLowerCase() == "none") {
            WeatherMixer.globalLightOverridden = false
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: setgl [r g b a|“none”]")
    }
}
