package net.torvald.terrarum.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.debuggerapp.ActorValueTracker

/**
 * Created by minjaesong on 2016-12-29.
 */
internal object AVTracker : ConsoleCommand {
    private val jPanelInstances = ArrayList<ActorValueTracker>()

    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            jPanelInstances.add(ActorValueTracker((Terrarum.ingame!! as TerrarumIngame).actorNowPlaying))
        }
        else {
            try {
                val actorID = args[1].toInt()

                if (INGAME.theGameHasActor(actorID)) {
                    jPanelInstances.add(ActorValueTracker(INGAME.getActorByID(actorID)))
                }
                else {
                    throw IllegalArgumentException()
                }
            }
            catch (e: NumberFormatException) {
                EchoError("Illegal actor ID input")
                return
            }
            catch (e1: IllegalArgumentException) {
                EchoError("No such actor with specified ID")
                return
            }
        }
    }

    override fun printUsage() {
        Echo("Pops up new window that provides real-time information about the actor's actor value")
    }

    fun update() {
        jPanelInstances.forEach { it.update() }
    }
}