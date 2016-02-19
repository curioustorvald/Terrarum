package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by minjaesong on 16-01-15.
 */
public class CommandInterpreter {

    public static void execute(String command) {
        CommandInput[] cmd = parse(command);

        for (CommandInput single_command : cmd) {
            try {
                if (single_command.getName().equalsIgnoreCase("auth")) {
                    Terrarum.game.auth.execute(single_command.toStringArray());
                }
                else if (single_command.getName().equalsIgnoreCase("qqq")) {
                    new QuitApp().execute(single_command.toStringArray());
                }
                else if (single_command.getName().equalsIgnoreCase("zoom")) {
                    new Zoom().execute(single_command.toStringArray());
                }
                else {
                    if (Terrarum.game.auth.C()) {
                        ConsoleCommand commandObj = CommandDict.getCommand(
                                single_command.getName().toLowerCase());
                        commandObj.execute(single_command.toStringArray());
                    }
                    else {
                        throw new NullPointerException(); // if not authorised, say "Unknown command"
                    }
                }
            }
            catch (NullPointerException e) {
                echoUnknownCmd(single_command.getName());
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
