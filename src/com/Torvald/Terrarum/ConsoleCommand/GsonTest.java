package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by minjaesong on 16-02-10.
 */
public class GsonTest implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            JsonElement avelem = new Gson().toJsonTree(Terrarum.game.getPlayer());

            String jsonString = avelem.toString();

            BufferedWriter bufferedWriter;
            FileWriter writer;
            try {
                writer = new FileWriter(Terrarum.defaultDir + "/Exports/" + args[1] + ".json");
                bufferedWriter = new BufferedWriter(writer);

                bufferedWriter.write(jsonString);
                bufferedWriter.close();

                new Echo().execute("GsonTest: exported to " + args[1] + ".json");
            }
            catch (IOException e) {
                new Echo().execute("GsonTest: IOException raised.");
                e.printStackTrace();
            }
        }
        else {
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        Echo echo = new Echo();
        echo.execute("Usage: gsontest filename-without-extension");
    }
}
