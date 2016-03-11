package com.Torvald.Terrarum;

import com.Torvald.ColourUtil.Col4096;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by minjaesong on 16-02-23.
 */
public class RoguelikeRandomiser {

    private static transient final int[] POTION_PRIMARY_COLSET = {15, 15, 8, 8, 0, 0};

    private static HashMap<Integer, Col4096> potionColours;
    private static HashMap<Col4096, Boolean> coloursDiscovered;

    private static ArrayList<Col4096> coloursTaken;


    private static transient final int POTION_HEAL_TIER1 = 0x00;
    private static transient final int POTION_HEAL_TIRE2 = 0x01;

    private static transient final int POTION_MAGIC_REGEN_TIER1 = 0x10;

    private static transient final int POTION_BERSERK_TIER1 = 0x20;

    public RoguelikeRandomiser() {
        potionColours = new HashMap<>();
        coloursTaken = new ArrayList<>();
    }



}
