package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Actors.Faction.Faction;
import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;

import java.util.HashSet;

/**
 * Created by minjaesong on 16-02-17.
 */
public class GetFactioning implements ConsoleCommand {

    private final String PRINT_INDENTATION = "    --> ";

    @Override
    public void execute(String[] args) {
        Echo echo = new Echo();

        if (args.length == 1) { // get all factioning data of player
            HashSet<Faction> factionSet = Terrarum.game.getPlayer().getAssignedFactions();

            int count = factionSet.size();
            echo.execute(String.valueOf(count) + Lang.pluralise(" faction", count) + " assigned.");

            for (Faction faction : factionSet) {
                echo.execute("Faction \"" + faction.getFactionName() + "\"");
                echo.execute("    Amicable");
                faction.getFactionAmicable().forEach(
                        s -> echo.execute(PRINT_INDENTATION + s)
                );

                echo.execute("    Explicit neutral");
                faction.getFactionNeutral().forEach(
                        s -> echo.execute(PRINT_INDENTATION + s)
                );

                echo.execute("    Hostile");
                faction.getFactionHostile().forEach(
                        s -> echo.execute(PRINT_INDENTATION + s)
                );

                echo.execute("    Fearful");
                faction.getFactionFearful().forEach(
                        s -> echo.execute(PRINT_INDENTATION + s)
                );
            }
        }
    }

    @Override
    public void printUsage() {

    }
}
