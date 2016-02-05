package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;

/**
 * Created by minjaesong on 16-01-24.
 */
public class TeleportPlayer implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        if (args.length != 3) {
            printUsage();
        }
        else {

            int x, y;
            try {
                x = new Integer((args[1])) * MapDrawer.TILE_SIZE + (MapDrawer.TILE_SIZE / 2);
                y = new Integer((args[2])) * MapDrawer.TILE_SIZE + (MapDrawer.TILE_SIZE / 2);
            }
            catch (NumberFormatException e) {
                new Echo().execute("Wrong number input.");
                return;
            }

            Game.getPlayer().setPosition(x, y);
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("Usage: teleport [x-tile] [y-tile]");
    }
}
