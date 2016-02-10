package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by minjaesong on 16-02-10.
 */
public class ExportAV implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            JsonElement avelem = new Gson().toJsonTree(Terrarum.game.getPlayer().getActorValue());
            String jsonString = avelem.toString();

            FileWriter writer;
            try {
                writer = new FileWriter(Terrarum.defaultDir + "/Exports/" + args[1] + ".json");
                writer.write(jsonString);
                writer.close();

                new Echo().execute("ExportAV: exported to " + args[1] + ".json");
            }
            catch (IOException e) {
                new Echo().execute("ExportAV: IOException raised.");
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
        echo.execute("Export ActorValue as JSON format.");
        echo.execute("Usage: exportav (id) filename-without-extension");
        echo.execute("blank ID for player");
    }
}
