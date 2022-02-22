package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilderSigrid

/**
 * Created by minjaesong on 2022-02-22.
 */
internal object DebugFillInventory : ConsoleCommand {
    override fun execute(args: Array<String>) {
        INGAME.actorNowPlaying?.let {
            it.inventory.nuke()
            PlayerBuilderSigrid.fillTestInventory(it.inventory)
        }
    }

    override fun printUsage() {
        Echo("Populates inventory of the currently playing actor with every item currently available. Will overwrite existing inventory.")
    }
}