package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.debuggerapp.ActorsLister
import net.torvald.terrarum.modulebasegame.Ingame
import java.util.*

/**
 * Created by minjaesong on 2016-12-29.
 */
internal object ActorsList : ConsoleCommand {
    private val jPanelInstances = ArrayList<ActorsLister>()

    override fun execute(args: Array<String>) {
        jPanelInstances.add(ActorsLister(
                (Terrarum.ingame!! as Ingame).actorContainerActive,
                (Terrarum.ingame!! as Ingame).actorContainerInactive)
        )
    }

    override fun printUsage() {
        Echo("Pops up new window that displays the list of actors currently in the game")
    }

    fun update() {
        jPanelInstances.forEach { it.update() }
    }
}