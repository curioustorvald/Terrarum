package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;

/**
 * Created by minjaesong on 16-01-16.
 */
class Echo implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        ((ConsoleWindow) Terrarum.game.consoleHandler.getUI())
                .sendMessage(args.toString());
    }

    public void execute(String single_line) {
        ((ConsoleWindow) Terrarum.game.consoleHandler.getUI())
                .sendMessage(single_line);
    }

    @Override
    public void printUsage() {

    }
}
