package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.Factionable
import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarumsansbitmap.gdx.GameFontBase

/**
 * Created by minjaesong on 2016-02-17.
 */
internal object GetFactioning : ConsoleCommand {
    val ccW = GameFontBase.toColorCode(0xFFFF)
    val ccY = GameFontBase.toColorCode(0xFE8F)
    val ccM = GameFontBase.toColorCode(0xEAFF)
    val ccG = GameFontBase.toColorCode(0x8F8F)
    val ccK = GameFontBase.toColorCode(0x888F)

    private val PRINT_INDENTATION = "$ccK    --> $ccW"

    override fun execute(args: Array<String>) {

        val error = Error()

        fun printOutFactioning(id: Int) {
            val a = Terrarum.ingame!!.getActorByID(id)
            if (a is Factionable) {
                Echo("$ccW== Faction assignment for $ccY${if (id == Player.PLAYER_REF_ID) "player" else id.toString()} $ccW==")
                println("[GetFactioning] == Faction assignment for '${if (id == Player.PLAYER_REF_ID) "player" else id.toString()}' ==")

                // get all factioning data of player
                val factionSet = a.faction

                if (factionSet.isEmpty()) {
                    Echo("The actor has empty faction set.")
                    println("[GetFactioning] The actor has empty faction set.")
                    return
                }

                val count = factionSet.size
                Echo("$ccG${count.toString()} $ccW${Lang.pluralise(" faction", count)} assigned.")
                println("[GetFactioning] ${count.toString()} ${Lang.pluralise(" faction", count)} assigned.")

                for (faction in factionSet) {
                    Echo("${ccW}faction $ccM${faction.factionName}")
                    println("[GetFactioning] faction '${faction.factionName}'")
                    Echo("$ccY    Amicable")
                    println("[GetFactioning]     Amicable")
                    faction.factionAmicable.forEach { s ->
                        Echo(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo("$ccY    Explicit neutral")
                    println("[GetFactioning]     Explicit neutral")
                    faction.factionNeutral.forEach { s ->
                        Echo(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo("$ccY    Hostile")
                    println("[GetFactioning]     Hostile")
                    faction.factionHostile.forEach { s ->
                        Echo(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    Echo("$ccY    Fearful")
                    println("[GetFactioning]     Fearful")
                    faction.factionFearful.forEach { s ->
                        Echo(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }
                }
            }
            else {
                EchoError("The actor is not factionable.")
                System.err.println("[GetFactioning] The actor is not factionable.")
            }
        }

        if (args.size == 1) {
            printOutFactioning(Player.PLAYER_REF_ID)
        }
        else {
            if (!args[1].isNum()) {
                EchoError("Invalid actor ID input.")
                System.err.println("[GetFactioning] Invalid actor ID input.")
                return
            }
            try {
                val actorID = args[1].toInt()
                printOutFactioning(actorID)
            }
            catch (e: IllegalArgumentException) {
                EchoError("${args[1]}: no actor with this ID.")
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
