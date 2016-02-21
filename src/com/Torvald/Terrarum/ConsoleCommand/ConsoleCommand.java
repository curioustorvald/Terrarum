package com.Torvald.Terrarum.ConsoleCommand;

/**
 * Created by minjaesong on 16-01-15.
 */
interface ConsoleCommand {

    void execute(String[] args) throws Exception;

    void printUsage();

}
