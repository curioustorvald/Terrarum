package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;

/**
 * Created by minjaesong on 16-01-25.
 */
public class Zoom implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {

            float zoom;
            try {
                zoom = new Float(args[1]);
            }
            catch (NumberFormatException e) {
                new Echo().execute("Wrong number input.");
                return;
            }

            if (zoom < Game.ZOOM_MIN) {
                zoom = Game.ZOOM_MIN;
            }
            else if (zoom > Game.ZOOM_MAX) {
                zoom = Game.ZOOM_MAX;
            }

            Game.screenZoom = zoom;

            System.gc();

            new Echo().execute("Set screen zoom to " + String.valueOf(zoom));
        }
        else {
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("Usage: zoom [zoom]");
    }
}
