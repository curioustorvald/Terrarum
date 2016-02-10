package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-01-23.
 *
 * 12-bit RGB
 */
public class Col4096 {

    private short data;

    /**
     * Create new Col4096 format.
     * @param data 0xARGB
     */
    public Col4096(int data) {
        this.data = (short) data;
    }

    /**
     * Create Col4096 colour and convert it to Slick Color
     * @param i
     * @return
     */
    public Color create(int i) {
        if (i > 0xFFF) {
            int a = (i & 0xF000) >> 12;
            int r = (i & 0x0F00) >> 8;
            int g = (i & 0x00F0) >> 4;
            int b = i & 0x000F;

            return new Color(
                    (r << 4) | r
                    , (g << 4) | g
                    , (b << 4) | b
                    , (a << 4) | a
            );
        }
        else {
            int r = (i & 0xF00) >> 8;
            int g = (i & 0x0F0) >> 4;
            int b = i & 0x00F;

            return new Color(
                    (r << 4) | r
                    , (g << 4) | g
                    , (b << 4) | b
            );
        }
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
    public short getShort() {
        return data;
    }

}
