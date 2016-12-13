package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.Factionable
import net.torvald.terrarum.gameactors.Player

/**
 * Created by minjaesong on 16-02-17.
 */
internal object GetFactioning : ConsoleCommand {
    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccM = GameFontBase.colToCode["m"]
    val ccK = GameFontBase.colToCode["k"]
    val ccB = GameFontBase.colToCode["b"]

    private val PRINT_INDENTATION = "$ccK    --> $ccW"

    override fun execute(args: Array<String>) {

        val error = Error()

        fun printOutFactioning(id: Int) {
            val a = Terrarum.ingame.getActorByID(id)
            if (a is Factionable) {
                Echo.execute("$ccW== Faction assignment for $ccY${if (id == Player.PLAYER_REF_ID) "player" else id.toString()} $ccW==")
                println("[GetFactioning] == Faction assignment for '${if (id == Player.PLAYER_REF_ID) "player" else id.toString()}' ==")

                // get all factioning data of player
                val factionSet = a.faction

                if (factionSet.isEmpty()) {
                    Echo.execute("The actor has empty faction set.")
                    println("[GetFactioning] The actor has empty faction set.")
                    return
                }

                val count = factionSet.size
                Echo.execute("$ccG${count.toString()} $ccW${Lang.pluralise(" faction", count)} assigned.")
                println("[GetFactioning] ${count.toString()} ${Lang.pluralise(" faction", count)} assigned.")

                for (faction in factionSet) {
                    Echo.execute("${ccW}faction $ccM${faction.factionName}")
                    println("[GetFactioning] faction '${faction.factionName}'")
                    Echo.execute("$ccY    Amicable")
                    println("[GetFactioning]     Amicable")
                    faction.factionAmicable.forEach { s ->
                        Echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo.execute("$ccY    Explicit neutral")
                    println("[GetFactioning]     Explicit neutral")
                    faction.factionNeutral.forEach { s ->
                        Echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo.execute("$ccY    Hostile")
                    println("[GetFactioning]     Hostile")
                    faction.factionHostile.forEach { s ->
                        Echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo.execute("$ccY    Fearful")
                    println("[GetFactioning]     Fearful")
                    faction.factionFearful.forEach { s ->
                        Echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }
                }
            }
            else {
                Error.execute("The actor is not factionable.")
                System.err.println("[GetFactioning] The actor is not factionable.")
            }
        }

        if (args.size == 1) {
            printOutFactioning(Player.PLAYER_REF_ID)
        }
        else {
            if (!args[1].isNum()) {
                Error.execute("Invalid actor ID input.")
                System.err.println("[GetFactioning] Invalid actor ID input.")
                return
            }
            try {
                val actorID = args[1].toInt()
                printOutFactioning(actorID)
            }
            catch (e: IllegalArgumentException) {
                Error.execute("${args[1]}: no actor with this ID.")
                System.err.println("[GetFactioning] ${args[1]}: no actor with this ID.")
            }
        }

    }

    fun String.isNum(): Boolean {
        try {
            this.toInt()
            return true
        }
        catch (e: NumberFormatException) {
            return false
        }
    }

    override fun printUsage() {

    }
}
