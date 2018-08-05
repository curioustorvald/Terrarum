package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-06-16.
 */
internal object Seed : ConsoleCommand {

    override fun execute(args: Array<String>) {
        Echo("Map$ccW: $ccG${(Terrarum.ingame!!.world).generatorSeed}")
        println("[seed] Map$ccW: $ccG${(Terrarum.ingame!!.world).generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo("prints out the generator seed of the current game.")
    }
}