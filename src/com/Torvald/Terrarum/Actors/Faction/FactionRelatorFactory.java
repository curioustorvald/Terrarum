package com.Torvald.Terrarum.Actors.Faction;

import com.Torvald.JsonGetter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by minjaesong on 16-02-15.
 */
public class FactionRelatorFactory {

    private static final String JSONPATH = "./res/raw/";

    public Faction build(String filename) throws IOException {
        JsonObject jsonObj = JsonGetter.readJson(JSONPATH + filename);
        Faction factionObj = new Faction(jsonObj.get("factionname").getAsString());


        jsonObj.get("factionamicable").getAsJsonArray().forEach(
                s -> factionObj.addFactionAmicable(s.getAsString())
        );
        jsonObj.get("factionneutral").getAsJsonArray().forEach(
                s -> factionObj.addFactionNeutral(s.getAsString())
        );
        jsonObj.get("factionhostile").getAsJsonArray().forEach(
                s -> factionObj.addFactionHostile(s.getAsString())
        );
        jsonObj.get("factionfearful").getAsJsonArray().forEach(
                s -> factionObj.addFactionFearful(s.getAsString())
        );

        return factionObj;
    }

}
