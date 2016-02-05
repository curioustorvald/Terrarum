package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;

/**
 * Created by minjaesong on 16-01-16.
 */
class Echo implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        ((ConsoleWindow) Game.consoleHandler.getUI())
                .sendMessage(args.toString());
    }

    public void execute(String single_line) {
        ((ConsoleWindow) Game.consoleHandler.getUI())
                .sendMessage(single_line);
    }

    @Override
    public void printUsage() {

    }
}
