package net.torvald.terrarum.console

import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum

import java.util.HashSet

/**
 * Created by minjaesong on 16-02-17.
 */
class GetFactioning : ConsoleCommand {

    private val PRINT_INDENTATION = "    --> "

    override fun execute(args: Array<String>) {
        val echo = Echo()

        if (args.size == 1) {
            // get all factioning data of player
            val factionSet = Terrarum.game.player.faction

            if (factionSet.isEmpty()) {
                echo.execute("The actor has empty faction set.")
                return
            }

            val count = factionSet.size
            echo.execute(count.toString() + Lang.pluralise(" faction", count) + " assigned.")

            for (faction in factionSet) {
                echo.execute("faction “${faction.factionName}”")
                echo.execute("    Amicable")
                faction.factionAmicable.forEach { s -> echo.execute(PRINT_INDENTATION + s) }

                echo.execute("    Explicit neutral")
                faction.factionNeutral.forEach { s -> echo.execute(PRINT_INDENTATION + s) }

                echo.execute("    Hostile")
                faction.factionHostile.forEach { s -> echo.execute(PRINT_INDENTATION + s) }

                echo.execute("    Fearful")
                faction.factionFearful.forEach { s -> echo.execute(PRINT_INDENTATION + s) }
            }
        }
    }

    override fun printUsage() {

    }
}
