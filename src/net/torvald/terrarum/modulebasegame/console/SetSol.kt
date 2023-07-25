package net.torvald.terrarum.modulebasegame.console

import net.torvald.reflection.extortField
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.LightmapRenderer

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
                    LightmapRenderer.recalculate(
                        INGAME.extortField<ArrayList<ActorWithBody>>("visibleActorsRenderBehind")!! +
                                INGAME.extortField<ArrayList<ActorWithBody>>("visibleActorsRenderMiddle")!! +
                                INGAME.extortField<ArrayList<ActorWithBody>>("visibleActorsRenderMidTop")!! +
                                INGAME.extortField<ArrayList<ActorWithBody>>("visibleActorsRenderFront")!! +
                                INGAME.extortField<ArrayList<ActorWithBody>>("visibleActorsRenderOverlay")!!
                    )
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