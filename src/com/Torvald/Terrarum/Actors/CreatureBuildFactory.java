package com.Torvald.Terrarum.Actors;

import com.Torvald.JsonFetcher;
import com.Torvald.Rand.Fudge3;
import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.LangPack.Lang;
import com.google.gson.JsonObject;
import org.newdawn.slick.SlickException;

import java.io.IOException;

/**
 * Created by minjaesong on 16-02-05.
 */
public class CreatureBuildFactory {

    private static final String JSONPATH = "./res/raw/";

    public ActorWithBody build(String jsonFileName) throws IOException, SlickException {
        JsonObject jsonObj = JsonFetcher.readJson(JSONPATH + jsonFileName);
        ActorWithBody actor = new ActorWithBody();


        String[] elementsString = {
                  "racename"
                , "racenameplural"
        };

        String[] elementsFloat = {
                  "baseheight"
                , "basemass"
                , "accel"
                , "toolsize"
                , "encumbrance"
        };

        String[] elementsFloatVariable = {
                  "strength"
                , "speed"
                , "jumppower"
                , "scale"
                , "speed"
        };

        String[] elementsBoolean = {
                  "intelligent"
        };

        String[] elementsMultiplyFromOne = {
                "physiquemult"
        };


        setAVStrings(actor, elementsString, jsonObj);
        setAVFloats(actor, elementsFloat, jsonObj);
        setAVFloatsVariable(actor, elementsFloatVariable, jsonObj);
        setAVMultiplyFromOne(actor, elementsMultiplyFromOne, jsonObj);
        setAVBooleans(actor, elementsBoolean, jsonObj);

        actor.actorValue.set("accel", Player.WALK_ACCEL_BASE);
        actor.actorValue.set("accelmult", 1f);

        actor.inventory = new ActorInventory((int) actor.actorValue.get("encumberance"), true);


        return actor;
    }

    /**
     * Fetch and set actor values that have 'variable' appended. E.g. strength
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVFloatsVariable(ActorWithBody p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            float baseValue = jsonObject.get(s).getAsFloat();
            // roll fudge dice and get value [-3, 3] as [0, 6]
            int varSelected = new Fudge3().create(new HQRNG()).roll() + 3;
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
    private void setAVStrings(ActorWithBody p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            String key = jsonObject.get(s).getAsString();
            p.actorValue.set(s, Lang.get(key));
        }
    }

    /**
     * Fetch and set float actor values
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVFloats(ActorWithBody p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            p.actorValue.set(s, jsonObject.get(s).getAsFloat());
        }
    }

    /**
     * Fetch and set actor values that should multiplier be applied to the base value of 1.
     * E.g. physiquemult
     * @param p
     * @param elemSet
     * @param jsonObject
     */
    private void setAVMultiplyFromOne(ActorWithBody p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            float baseValue = 1f;
            // roll fudge dice and get value [-3, 3] as [0, 6]
            int varSelected = new Fudge3().create(new HQRNG()).roll() + 3;
            // get multiplier from json. Assuming percentile
            int multiplier = jsonObject.get(s).getAsJsonArray().get(varSelected).getAsInt();
            float realValue = baseValue * multiplier / 100f;

            p.actorValue.set(s, realValue);
        }
    }

    private void setAVBooleans(ActorWithBody p, String[] elemSet, JsonObject jsonObject) {
        for (String s : elemSet) {
            p.actorValue.set(s, jsonObject.get(s).getAsBoolean());
        }
    }
}
