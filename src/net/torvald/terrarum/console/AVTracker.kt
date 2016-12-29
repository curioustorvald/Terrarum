package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.debuggerapp.ActorValueTracker
import java.util.*

/**
 * Created by SKYHi14 on 2016-12-29.
 */
object AVTracker : ConsoleCommand {
    private val jPanelInstances = ArrayList<ActorValueTracker>()

    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            jPanelInstances.add(ActorValueTracker(Terrarum.ingame.player))
        }
        else {
            try {
                val actorID = args[1].toInt()

                if (Terrarum.ingame.hasActor(actorID)) {
                    jPanelInstances.add(ActorValueTracker(Terrarum.ingame.getActorByID(actorID)))
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
        jPanelInstances.forEach { it.setInfoLabel() }
    }
}