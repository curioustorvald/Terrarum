package com.Torvald.Terrarum.Actors;


import com.Torvald.Rand.Fudge3;
import com.Torvald.Rand.HighQualityRandom;
import com.google.gson.*;
import org.newdawn.slick.SlickException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by minjaesong on 16-02-03.
 */
public class PlayerBuildFactory {

    private static final String JSONPATH = "./res/raw/";
    private static String jsonString = new String();

    public Player build(String jsonFileName) throws IOException, SlickException {
        JsonObject jsonObj = readJson(jsonFileName);
        Player p = new Player();


        String[] elementsString = {
                "racename"
                , "racenameplural"
        };

        String[] elementsFloat = {
                "baseheight"
                , "basemass"
                , "toolsize"
                , "encumbrance"
        };

        String[] elementsFloatVariable = {
                "baseheight"
                , "strength"
                , "speed"
                , "jumppower"
                , "scale"
        };


        setAVStrings(p, elementsString, jsonObj);
        setAVFloats(p, elementsFloat, jsonObj);
        setAVFloatsVariable(p, elementsFloatVariable, jsonObj);


        p.inventory = new ActorInventory((int) p.actorValue.get("encumberance"), true);


        return p;
    }

    /**
     * Fetch and set actor values that have 'variable' appended. E.g. strength
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVFloatsVariable(Player p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            float baseValue = jsonObject.get(s).getAsFloat();
            // roll fudge dice and get value [-3, 3] as [0, 6]
            int varSelected = new Fudge3().create(new HighQualityRandom()).roll() + 3;
            // get multiplier from json. Assuming percentile
            int multiplier = jsonObject.get(s + "variable").getAsJsonArray().get(varSelected).getAsInt();
            float realValue = baseValue * multiplier / 100f;

            p.actorValue.set(s, realValue);
        }
    }

    /**
     * Fetch and set string actor values
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVStrings(Player p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            p.actorValue.set(s, jsonObject.get(s).getAsString());
        }
    }

    /**
     * Fetch and set float actor values
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVFloats(Player p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            p.actorValue.set(s, jsonObject.get(s).getAsFloat());
        }
    }

    private JsonObject readJson(String jsonFileName) throws IOException  {
        readJsonFileAsString(jsonFileName);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(jsonString).getAsJsonObject();

        return jsonObj;
    }

    private void readJsonFileAsString(String filename) throws IOException {
        Files.lines(
                FileSystems.getDefault().getPath(JSONPATH + filename)
        ).forEach(this::strAppend);
    }

    private void strAppend( String s) {
        jsonString += s;
    }

}
