package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;

/**
 * Created by minjaesong on 16-01-19.
 */
public class ToggleNoClip implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        boolean status = Game.getPlayer().isNoClip();

        Game.getPlayer().setNoClip(!status);
        new Echo().execute("Set no-clip status to " + String.valueOf(!status));
    }

    @Override
    public void printUsage() {
        new Echo().execute("toggle no-clip status of player");
    }
}
