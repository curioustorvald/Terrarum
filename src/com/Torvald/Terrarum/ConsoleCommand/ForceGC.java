package com.Torvald.Terrarum.ConsoleCommand;

/**
 * Created by minjaesong on 16-01-18.
 */
public class ForceGC implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        System.gc();
        new Echo().execute("Invoked System.gc");
    }

    @Override
    public void printUsage() {
        new Echo().execute("Invoke garbage collection of JVM.");
    }
}
