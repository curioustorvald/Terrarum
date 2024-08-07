package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2016-06-16.
 */
internal object Seed : ConsoleCommand {

    override fun execute(args: Array<String>) {
        Echo("Map$ccW: $ccG${(INGAME.world).generatorSeed}")
        println("[seed] Map$ccW: $ccG${(INGAME.world).generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo("prints out the generator seed of the current game.")
    }
}