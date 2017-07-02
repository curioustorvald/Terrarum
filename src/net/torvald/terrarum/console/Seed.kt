package net.torvald.terrarum.console

import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-06-16.
 */
internal object Seed : ConsoleCommand {

    override fun execute(args: Array<String>) {
        Echo("Map$ccW: $ccG${TerrarumGDX.ingame!!.world.generatorSeed}")
        println("[seed] Map$ccW: $ccG${TerrarumGDX.ingame!!.world.generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo("prints out the generator seed of the current game.")
    }
}