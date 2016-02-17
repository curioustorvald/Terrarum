package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-02-17.
 */
public class SetGlobalLightLevel implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 4) {
            try {
                int r = new Integer(args[1]);
                int g = new Integer(args[2]);
                int b = new Integer(args[3]);
                int GL = (r << 16) | (g << 8) | b;

                Terrarum.game.map.setGlobalLight(GL);
            }
            catch (NumberFormatException e) {
                new Echo().execute("Wrong number input.");
            }
        }
        else{
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("Usage: setgl r g b");
    }
}
