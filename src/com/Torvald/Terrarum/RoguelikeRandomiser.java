package com.Torvald.Terrarum;

import com.Torvald.ColourUtil.Col4096;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by minjaesong on 16-02-23.
 */
public class RoguelikeRandomiser {

    private static final int[] POTION_PRIMARY_COLSET = {15, 15, 8, 8, 0, 0};

    private static Hashtable<Integer, Col4096> potionColours;
    private static ArrayList<Col4096> coloursTaken;


    private static final int POTION_HEAL_TIER1 = 0x00;
    private static final int POTION_HEAL_TIRE2 = 0x01;

    private static final int POTION_MAGIC_REGEN_TIER1 = 0x10;

    private static final int POTION_BERSERK_TIER1 = 0x20;

    public RoguelikeRandomiser() {
        potionColours = new Hashtable<>();
        coloursTaken = new ArrayList<>();
    }



}
