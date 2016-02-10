package com.Torvald.Terrarum.Actors;


import org.newdawn.slick.SlickException;

import java.io.IOException;

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
