package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;

/**
 * Created by minjaesong on 16-01-16.
 */
public class CodexEdictis implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 1) {
            printList();
        }
        else{
            try {
                ConsoleCommand commandObj = CommandDict.getCommand(args[1].toLowerCase());
                commandObj.printUsage();
            }
            catch (NullPointerException e) {
                new Echo().execute("Codex: Unknown command: " + args[1]);
            }
        }
    }

    @Override
    public void printUsage() {
        Echo echo = new Echo();
        echo.execute("Usage: codex (command)");
        echo.execute("shows how to use 'command'");
        echo.execute("leave blank to get list of available commands");
    }

    private void printList() {
        Echo echo = new Echo();
        echo.execute("Available commands");
        echo.execute("--------------------------------");

        CommandDict.dict.keySet().forEach((s) -> echo.execute(s));


        echo.execute("--------------------------------");
    }

}
