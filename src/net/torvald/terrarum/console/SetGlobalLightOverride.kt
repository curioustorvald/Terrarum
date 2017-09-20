package net.torvald.terrarum.console

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2016-02-17.
 */
internal object SetGlobalLightOverride : ConsoleCommand {

    var lightOverride = false
        private set

    override fun execute(args: Array<String>) {
        if (args.size == 5) {
            try {
                val r = args[1].toFloat()
                val g = args[2].toFloat()
                val b = args[3].toFloat()
                val a = args[4].toFloat()
                val GL = Color(r, g, b, a)

                lightOverride = true
                Terrarum.ingame!!.world.globalLight = GL
            }
            catch (e: NumberFormatException) {
                Echo("Wrong number input.")
            }
            catch (e1: IllegalArgumentException) {
                Echo("Range: 0-" + LightmapRenderer.CHANNEL_MAX + " per channel")
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: setgl [r g b a|“none”]")
    }
}
