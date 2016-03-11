package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-02-11.
 */
public class Col216 implements LimitedColours {

    private byte data;
    private static transient final int[] LOOKUP = {0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF};

    public static final int MUL = 6;
    public static final int MUL_2 = MUL * MUL;
    public static final int MAX_STEP = MUL - 1;
    public static final int COLOUR_DOMAIN_SIZE = MUL_2 * MUL;

    /**
     *
     * @param data
     */
    public Col216(byte data) {
        create(data);
    }

    /**
     *
     * @param r 0-5
     * @param g 0-5
     * @param b 0-5
     */
    public Col216(int r, int g, int b) {
        create(r, g, b);
    }

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
        return new Color(LOOKUP[r], LOOKUP[g], LOOKUP[b]);
    }

    @Override
    public void create(int raw) {
        assertRaw(raw);
        data = (byte) raw;
    }

    @Override
    public void create(int r, int g, int b) {
        assertRGB(r, g, b);
        data = (byte) (MUL_2 * r + MUL * g + b);
    }

    public byte getRaw() { return data; }

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
