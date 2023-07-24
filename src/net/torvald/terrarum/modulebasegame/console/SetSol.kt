package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.weather.WeatherMixer

/**
 * Created by minjaesong on 2023-07-25.
 */
internal object SetSol : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            if (args[1].trim().lowercase() == "none") {
                WeatherMixer.forceSolarElev = null
            }
            else {
                try {
                    val solarAngle = args[1].toDouble().coerceIn(-75.0..75.0)
                    WeatherMixer.forceSolarElev = solarAngle
                }
                catch (e: NumberFormatException) {
                    Echo("Wrong number input.")
                }
                catch (e1: IllegalArgumentException) {
                    Echo("Range: -75.0-75.0")
                }
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: setsol <-75.0..75.0 or 'none'>")
    }
}