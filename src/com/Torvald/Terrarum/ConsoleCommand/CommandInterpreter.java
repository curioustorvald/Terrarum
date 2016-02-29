package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by minjaesong on 16-01-15.
 */
public class CommandInterpreter {

    private static String[] commandsAvailableWOAuth = {"auth", "qqq", "zoom", "setlocale", "getlocale"};

    public static void execute(String command) {
        CommandInput[] cmd = parse(command);

        for (CommandInput single_command : cmd) {
            ConsoleCommand commandObj = null;
            try {
                if (Arrays.asList(commandsAvailableWOAuth).contains(single_command.getName().toLowerCase())) {
                    commandObj = CommandDict.getCommand(single_command.getName().toLowerCase());
                }
                else {
                    if (Terrarum.game.auth.b()) {
                        commandObj = CommandDict.getCommand(
                                single_command.getName().toLowerCase()
                        );
                    }
                    else {
                        System.out.println("ee1");
                        throw new NullPointerException(); // if not authorised, say "Unknown command"
                    }
                }
            }
            catch (NullPointerException e) {

            }
            finally {
                try {
                    if (commandObj != null) {
                        commandObj.execute(single_command.toStringArray());
                    }
                    else {
                        echoUnknownCmd(single_command.getName());
                        System.out.println("ee3");
                    }
                }
                catch (Exception e) {
                    System.out.println("[CommandInterpreter] :");
                    e.printStackTrace();
                    new Echo().execute(Lang.get("ERROR_GENERIC_TEXT"));
                }
            }
        }
    }

    private static CommandInput[] parse(String input) {
        Pattern patternCommands = Pattern.compile("[^;]+");
        Pattern patternTokensInCommand = Pattern.compile("[\"'][^;]+[\"']|[^ ]+");

        ArrayList<String> commands = new ArrayList<>();

        // split multiple commands
        Matcher m = patternCommands.matcher(input);
        while (m.find()) commands.add(m.group());

        // split command tokens from a command
        CommandInput[] parsedCommands = new CommandInput[commands.size()];


        for (int i = 0; i < parsedCommands.length; i++) {
            ArrayList<String> tokens = new ArrayList<>();

            m = patternTokensInCommand.matcher(commands.get(i));
            while (m.find()) {
                String regexGroup = m.group().replaceAll("[\"\']", "");
                tokens.add(regexGroup);
            }

            // create new command
            parsedCommands[i] = new CommandInput(tokens.toArray());

        }

        return parsedCommands;
    }

    static void echoUnknownCmd(String cmdname) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        new Echo().execute(
                formatter.format(Lang.get("DEV_MESSAGE_CONSOLE_COMMAND_UNKNOWN")
                        , cmdname
                ).toString()
        );
    }

}

class CommandInput {
    private String[] tokens;

    CommandInput(Object[] o) {
        tokens = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            tokens[i] = (String) o[i];
        }
    }

    String[] toStringArray() {
        return tokens;
    }

    String getName() {
        return tokens[0];
    }

    int getArgsCount() {
        return tokens.length;
    }
}
