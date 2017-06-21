package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-06-16.
 */
internal object Seed : ConsoleCommand {
    val ccG = 0.toChar()//GameFontBase.colToCode["g"]
    val ccW = 0.toChar()//GameFontBase.colToCode["w"]
    val ccY = 0.toChar()//GameFontBase.colToCode["y"]

    override fun execute(args: Array<String>) {
        Echo("Map$ccW: $ccG${TerrarumGDX.ingame!!.world.generatorSeed}")
        println("[seed] Map$ccW: $ccG${TerrarumGDX.ingame!!.world.generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo("prints out the generator seed of the current game.")
    }
}