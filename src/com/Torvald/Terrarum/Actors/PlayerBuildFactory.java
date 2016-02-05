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
        Player p = (Player) (new CreatureBuildFactory().build("CreatureHuman"));

        // attach sprite

        // do etc.

        return p;
    }

}
