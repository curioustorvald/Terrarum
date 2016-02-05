package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;

/**
 * Created by minjaesong on 16-01-15.
 */
public class CommandInterpreter {

    public static void execute(String command) {
        CommandInput[] cmd = parse(command);

        for (CommandInput single_command : cmd) {
            try {
                ConsoleCommand commandObj = CommandDict.getCommand(single_command.getName().toLowerCase());
                commandObj.execute(single_command.toStringArray());
            }
            catch (NullPointerException e) {
                new Echo().execute("Unknown command: " + single_command.getName());
            }
        }
    }

    private static CommandInput[] parse(String input) {
        String[] commands = input.split(";[ ]?"); // split multiple commands (e.g. respawn player; setav player speed 5)

        CommandInput[] ret = new CommandInput[commands.length];
        for (int i = 0; i < commands.length; i++) {
            ret[i] = new CommandInput(commands[i].split(" "));
        }

        return ret;
    }

}

class CommandInput {
    private String[] args;
    private int argsCount = 0;

    CommandInput(String[] s) {
        args = s;
    }

    String[] toStringArray() {
        return args;
    }

    String getName() {
        return args[0];
    }

    int getArgsCount() {
        return argsCount;
    }
}
