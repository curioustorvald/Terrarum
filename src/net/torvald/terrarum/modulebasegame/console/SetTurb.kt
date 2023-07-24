package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.weather.WeatherMixer

/**
 * Created by minjaesong on 2023-07-25.
 */
object SetTurb : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            if (args[1].trim().lowercase() == "none") {
                WeatherMixer.forceTurbidity = null
            }
            else {
                try {
                    val turbidity = args[1].toDouble().coerceIn(1.0..10.0)
                    WeatherMixer.forceTurbidity = turbidity
                }
                catch (e: NumberFormatException) {
                    Echo("Wrong number input.")
                }
                catch (e1: IllegalArgumentException) {
                    Echo("Range: 1.0-10.0")
                }
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: setturb <1.0..10.0 or 'none'>")
    }
}