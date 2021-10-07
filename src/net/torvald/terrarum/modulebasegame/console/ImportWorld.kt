package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.serialise.ReadActor
import net.torvald.terrarum.serialise.ReadWorld
import java.io.IOException

/**
 * Created by minjaesong on 2021-08-25.
 */
object ImportWorld : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val reader = java.io.FileReader(App.defaultDir + "/Exports/${args[1]}.json")
                ReadWorld.readWorldAndSetNewWorld(Terrarum.ingame!! as TerrarumIngame, reader)
                Echo("Importworld: imported a world from ${args[1]}.json")
            }
            catch (e: IOException) {
                Echo("Importworld: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: Importworld filename-without-extension")
    }
}
/*
object ImportActor : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val reader = java.io.FileReader(App.defaultDir + "/Exports/${args[1]}.json")
                ReadActor.readActorAndAddToWorld(Terrarum.ingame!! as TerrarumIngame, reader)
                Echo("Importactor: imported an actor from ${args[1]}.json")
            }
            catch (e: IOException) {
                Echo("Importactor: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: Importactor filename-without-extension")
    }
}*/