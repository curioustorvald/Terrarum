package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.MapDrawer.LightmapRenderer;
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
                char GL = LightmapRenderer.constructRGBFromInt(r, g, b);

                Terrarum.game.map.setGlobalLight(GL);
            }
            catch (NumberFormatException e) {
                new Echo().execute("Wrong number input.");
            }
            catch (IllegalArgumentException e1) {
                new Echo().execute("Range: 0-" + LightmapRenderer.getCHANNEL_MAX() + " per channel");
            }
        }
        else if (args.length == 2) {
            try {
                char GL = (char) (new Integer(args[1]).intValue());

                if (GL < 0 || GL >= LightmapRenderer.getCOLOUR_DOMAIN_SIZE()) {
                    new Echo().execute("Range: 0-" + (LightmapRenderer.getCOLOUR_DOMAIN_SIZE() - 1));
                }
                else {
                    Terrarum.game.map.setGlobalLight(GL);
                }
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
        new Echo().execute("Usage: setgl [raw_value|r g b]");
    }
}
