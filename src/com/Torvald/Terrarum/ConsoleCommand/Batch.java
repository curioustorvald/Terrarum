package com.Torvald.Terrarum.ConsoleCommand;

import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by minjaesong on 16-03-07.
 */
public class Batch implements ConsoleCommand {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 2) {
            Files.lines(FileSystems.getDefault().getPath(args[1])).forEach
                    (CommandInterpreter::execute);
        }
        else {
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("batch path/to/batch.txt");
    }
}
