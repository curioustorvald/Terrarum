package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.debuggerapp.ActorsLister
import java.util.*

/**
 * Created by SKYHi14 on 2016-12-29.
 */
object ActorsList : ConsoleCommand {
    private val jPanelInstances = ArrayList<ActorsLister>()

    override fun execute(args: Array<String>) {
        jPanelInstances.add(ActorsLister(
                Terrarum.ingame.actorContainer,
                Terrarum.ingame.actorContainerInactive)
        )
    }

    override fun printUsage() {
        Echo("Pops up new window that displays the list of actors currently in the game")
    }

    fun update() {
        jPanelInstances.forEach { it.update() }
    }
}