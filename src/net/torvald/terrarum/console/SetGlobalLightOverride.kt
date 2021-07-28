package net.torvald.terrarum.console

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.weather.WeatherMixer

/**
 * Created by minjaesong on 2016-02-17.
 */
internal object SetGlobalLightOverride : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 5) {
            try {
                val r = args[1].toFloat()
                val g = args[2].toFloat()
                val b = args[3].toFloat()
                val a = args[4].toFloat()
                val GL = Cvec(r, g, b, a)

                WeatherMixer.globalLightOverridden = true
                (Terrarum.ingame!!.world).globalLight = GL
            }
            catch (e: NumberFormatException) {
                Echo("Wrong number input.")
            }
            catch (e1: IllegalArgumentException) {
                Echo("Range: 0.0-1.0+ per channel")
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
