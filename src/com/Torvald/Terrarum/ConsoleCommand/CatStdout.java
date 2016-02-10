package com.Torvald.Terrarum.ConsoleCommand;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by minjaesong on 16-02-10.
 */
public class CatStdout implements ConsoleCommand {
    @Override
    public void execute(String[] args) {

        Echo echo = new Echo();

        if (args.length == 1) {
            printUsage();
            return;
        }

        try {
            Files.lines(FileSystems.getDefault().getPath(args[1])).forEach(echo::execute);
        }
        catch (IOException e) {
            echo.execute("CatStdout: could not read file -- IOException");
        }

    }

    @Override
    public void printUsage() {
        new Echo().execute("usage: cat 'path/to/text/file");
    }
}
