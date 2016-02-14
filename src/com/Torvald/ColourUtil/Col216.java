package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-02-11.
 */
public class Col216 implements LimitedColours {

    private byte data;
    private static int[] LOOKUP = {0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF};

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
        int r = LOOKUP[(raw / 36)];
        int g = LOOKUP[((raw % 36) / 6)];
        int b = LOOKUP[raw % 6];

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
        data = (byte) (36 * r + 6 * g + b);
    }

    public byte getRaw() { return data; }

    private void assertRaw(int i) {
        if (i > 0xFF || i < 0) {
            System.out.println("i: " + String.valueOf(i));
            throw new IllegalArgumentException();
        }
    }

    private void assertRGB(int r, int g, int b) {
        if (r > 5 || g > 5 || b > 5 || r < 0 || g < 0 || b < 0) {
            System.out.println("r: " + String.valueOf(r));
            System.out.println("g: " + String.valueOf(g));
            System.out.println("b: " + String.valueOf(b));
            throw new IllegalArgumentException();
        }
    }
}
