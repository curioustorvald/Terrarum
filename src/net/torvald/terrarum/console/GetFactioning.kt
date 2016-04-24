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
class GetFactioning : ConsoleCommand {
    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccM = GameFontBase.colToCode["m"]
    val ccK = GameFontBase.colToCode["k"]
    val ccB = GameFontBase.colToCode["b"]

    private val PRINT_INDENTATION = "$ccK    --> $ccW"

    override fun execute(args: Array<String>) {
        val echo = Echo()

        fun printOutFactioning(id: Int) {
            val a = Terrarum.game.getActor(id)
            if (a is Factionable) {
                echo.execute("$ccW== Faction assignment for $ccY${if (id == Player.PLAYER_REF_ID) "player" else id.toString()} $ccW==")
                println("[GetFactioning] == Faction assignment for '${if (id == Player.PLAYER_REF_ID) "player" else id.toString()}' ==")

                // get all factioning data of player
                val factionSet = a.faction

                if (factionSet.isEmpty()) {
                    echo.execute("The actor has empty faction set.")
                    println("[GetFactioning] The actor has empty faction set.")
                    return
                }

                val count = factionSet.size
                echo.execute("$ccG${count.toString()} $ccW${Lang.pluralise(" faction", count)} assigned.")
                println("[GetFactioning] ${count.toString()} ${Lang.pluralise(" faction", count)} assigned.")

                for (faction in factionSet) {
                    echo.execute("${ccW}faction $ccM${faction.factionName}")
                    println("[GetFactioning] faction '${faction.factionName}'")
                    echo.execute("$ccY    Amicable")
                    println("[GetFactioning]     Amicable")
                    faction.factionAmicable.forEach { s ->
                        echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    echo.execute("$ccY    Explicit neutral")
                    println("[GetFactioning]     Explicit neutral")
                    faction.factionNeutral.forEach { s ->
                        echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    echo.execute("$ccY    Hostile")
                    println("[GetFactioning]     Hostile")
                    faction.factionHostile.forEach { s ->
                        echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }

                    echo.execute("$ccY    Fearful")
                    println("[GetFactioning]     Fearful")
                    faction.factionFearful.forEach { s ->
                        echo.execute(PRINT_INDENTATION + s)
                        println("[GetFactioning]     --> $s")
                    }
                }
            }
            else {
                echo.error("The actor is not factionable.")
                System.err.println("[GetFactioning] The actor is not factionable.")
            }
        }

        if (args.size == 1) {
            printOutFactioning(Player.PLAYER_REF_ID)
        }
        else {
            if (!args[1].isNum()) {
                echo.error("Invalid actor ID input.")
                System.err.println("[GetFactioning] Invalid actor ID input.")
                return
            }
            try {
                val actorID = args[1].toInt()
                printOutFactioning(actorID)
            }
            catch (e: IllegalArgumentException) {
                echo.error("${args[1]}: no actor with this ID.")
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
