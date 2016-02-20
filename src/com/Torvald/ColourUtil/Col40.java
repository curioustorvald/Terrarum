package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-02-20.
 */
public class Col40 implements LimitedColours {

    private char data;
    private static int[] LOOKUP = {0,7,13,20,26,33,39,46,52,59,65,72,78,85,92,98,105,111,118,124
            ,131,137,144,150,157,163,170,177,183,190,196,203,209,216,222,229,235,242,248,255};

    public static final int MUL = 40;
    public static final int MUL_2 = MUL * MUL;
    public static final int MAX_STEP = MUL - 1;
    public static final int COLOUR_DOMAIN_SIZE = MUL_2 * MUL;

    @Override
    public Color createSlickColor(int raw) {
        assertRaw(raw);
        int r = raw / MUL_2;
        int g = (raw % MUL_2) / MUL;
        int b = raw % MUL;

        return createSlickColor(r, g, b);
    }

    @Override
    public Color createSlickColor(int r, int g, int b) {
        assertRGB(r, g, b);
        return new Color((LOOKUP[r] << 16) | (LOOKUP[g] << 8) | LOOKUP[b]);
    }

    @Override
    public void create(int raw) {
        assertRaw(raw);
        data = (char) raw;
    }

    @Override
    public void create(int r, int g, int b) {
        assertRGB(r, g, b);
        data = (char) (MUL_2 * r + MUL * g + b);
    }

    public char getRaw() { return data; }

    private void assertRaw(int i) {
        if (i >= COLOUR_DOMAIN_SIZE || i < 0) {
            System.out.println("i: " + String.valueOf(i));
            throw new IllegalArgumentException();
        }
    }

    private void assertRGB(int r, int g, int b) {
        if (r > MAX_STEP || g > MAX_STEP || b > MAX_STEP || r < 0 || g < 0 || b < 0) {
            System.out.println("r: " + String.valueOf(r));
            System.out.println("g: " + String.valueOf(g));
            System.out.println("b: " + String.valueOf(b));
            throw new IllegalArgumentException();
        }
    }
}
