package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-01-23.
 *
 * 12-bit RGB
 */
public class Col4096 implements LimitedColours {

    private short data;

    /**
     *
     * @param data
     */
    public Col4096(int data) {
        create(data);
    }

    /**
     *
     * @param r 0-15
     * @param g 0-15
     * @param b 0-15
     */
    public Col4096(int r, int g, int b) {
        create(r, g, b);
    }

    /**
     * Create Col4096 colour and convert it to Slick Color
     * @param i
     * @return
     */
    public Color createSlickColor(int i) {
        assertRaw(i);

        int a, r, g, b;

        r = (i & 0xF00) >> 8;
        g = (i & 0x0F0) >> 4;
        b = i & 0x00F;

        if (i > 0xFFF) {
            a = (i & 0xF000) >> 12;

            return new Color(
                    (r << 4) | r
                    , (g << 4) | g
                    , (b << 4) | b
                    , (a << 4) | a
            );
        }
        else {
            return new Color(
                    (r << 4) | r
                    , (g << 4) | g
                    , (b << 4) | b
            );
        }
    }

    @Override
    public Color createSlickColor(int r, int g, int b) {
        assertARGB(0, r, g, b);
        return createSlickColor(r << 8 | g << 4 | b);
    }

    public Color createSlickColor(int a, int r, int g, int b) {
        assertARGB(a, r, g, b);
        return createSlickColor(a << 12 |r << 8 | g << 4 | b);
    }

    @Override
    public void create(int raw) {
        assertRaw(raw);
        data = (short) (raw & 0xFFFF);
    }

    @Override
    public void create(int r, int g, int b) {
        assertARGB(0, r, g, b);
        data = (short) (r << 8 | g << 4 | b);
    }

    public void create(int a, int r, int g, int b) {
        assertARGB(a, r, g, b);
        data = (short) (a << 12 | r << 8 | g << 4 | b);
    }

    /**
     * Convert to 3 byte values, for raster imaging.
     * @return byte[RR, GG, BB] e.g. 0x4B3 -> 0x44, 0xBB, 0x33
     */
    public byte[] toByteArray() {
        byte[] ret = new byte[3];
        int r = (data & 0xF00) >> 8;
        int g = (data & 0x0F0) >> 4;
        int b = data & 0x00F;

        ret[0] = (byte) ((r << 4) | r);
        ret[1] = (byte) ((g << 4) | g);
        ret[2] = (byte) ((b << 4) | b);

        return ret;
    }

    /**
     * Retrieve raw ARGB value
     * @return 0xARGB
     */
    public short getRaw() {
        return data;
    }

    private void assertRaw(int i) {
        if (i > 0xFFFF || i < 0) {
            System.out.println("i: " + String.valueOf(i));
            throw new IllegalArgumentException();
        }
    }

    private void assertARGB(int a, int r, int g, int b) {
        if (a > 16 || r > 16 || g > 16 || b > 16 || r < 0 || g < 0 || b < 0 || a < 0) {
            System.out.println("a: " + String.valueOf(a));
            System.out.println("r: " + String.valueOf(r));
            System.out.println("g: " + String.valueOf(g));
            System.out.println("b: " + String.valueOf(b));
            throw new IllegalArgumentException();
        }
    }

}
