package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-06-16.
 */
internal object Seed : ConsoleCommand {
    val ccG = GameFontBase.colToCode["g"]
    val ccW = GameFontBase.colToCode["w"]
    val ccY = GameFontBase.colToCode["y"]

    override fun execute(args: Array<String>) {
        Echo("Map$ccW: $ccG${Terrarum.ingame!!.world.generatorSeed}")
        println("[seed] Map$ccW: $ccG${Terrarum.ingame!!.world.generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo("prints out the generator seed of the current game.")
    }
}