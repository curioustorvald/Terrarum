package com.Torvald.Terrarum.ConsoleCommand;

/**
 * Created by minjaesong on 16-01-15.
 */
public class QuitApp implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        System.exit(1);
    }

    @Override
    public void printUsage() {

    }
}
