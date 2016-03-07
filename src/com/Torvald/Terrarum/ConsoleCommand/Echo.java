package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;

import java.util.Arrays;
import java.util.List;

/**
 * Created by minjaesong on 16-01-16.
 */
class Echo implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        String[] argsWoHeader = new String[args.length - 1];
        for (int i = 0; i < argsWoHeader.length; i++)
            argsWoHeader[i] = args[i + 1];

        Arrays.asList(argsWoHeader).forEach(
                ((ConsoleWindow) Terrarum.game.consoleHandler.getUI())::sendMessage
        );
    }

    public void execute(String single_line) {
        ((ConsoleWindow) Terrarum.game.consoleHandler.getUI())
                .sendMessage(single_line);
    }

    @Override
    public void printUsage() {

    }
}
