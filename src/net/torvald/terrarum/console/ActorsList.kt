package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.debuggerapp.ActorsLister
import java.util.*

/**
 * Created by minjaesong on 2016-12-29.
 */
internal object ActorsList : ConsoleCommand {
    private val jPanelInstances = ArrayList<ActorsLister>()

    override fun execute(args: Array<String>) {
        jPanelInstances.add(ActorsLister(
                TerrarumGDX.ingame!!.actorContainer,
                TerrarumGDX.ingame!!.actorContainerInactive)
        )
    }

    override fun printUsage() {
        Echo("Pops up new window that displays the list of actors currently in the game")
    }

    fun update() {
        jPanelInstances.forEach { it.update() }
    }
}