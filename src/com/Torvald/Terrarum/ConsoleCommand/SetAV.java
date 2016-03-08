package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-01-15.
 */
class SetAV implements ConsoleCommand {

    @Override
    public void printUsage() {
        Echo echo = new Echo();
        echo.execute("Set actor value of specific target to desired value.");
        echo.execute("Usage: setav (id) <av> <val>");
        echo.execute("blank ID for player");
    }

    @Override
    public void execute(String[] args) {
        Echo echo = new Echo();

        // setav <id or "player"> <av> <val>
        if (args.length != 4 && args.length != 3) {
            printUsage();
        }
        else if (args.length == 3) {
            Object val;

            try {
                val = new Integer(args[2]); // try for integer
            }
            catch (NumberFormatException e) {

                try {
                    val = new Float(args[2]); // try for float
                }
                catch (NumberFormatException ee) {
                    if (args[2].equalsIgnoreCase("__true")) {
                        val = new Boolean(true);
                    }
                    else if (args[2].equalsIgnoreCase("__false")) {
                        val = new Boolean(false);
                    }
                    else {
                        val = new String(args[2]); // string if not number
                    }
                }
            }

            Terrarum.game.getPlayer().getActorValue().set(args[1], val);
            echo.execute("Set " + args[1] + " to " + val);
        }
        else if (args.length == 4) {

        }

    }
}
