package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-01-23.
 */
public class Col12 {

    private short data;

    /**
     * Create new Col12 format
     * @param data 0x000-0xFFF, in RGB
     */
    public Col12(int data) {
        this.data = (short) data;
    }

    public Color create(int i) {
        if (i > 0xFFF || i < 0) {
            throw new IllegalArgumentException("Colour range: #000 - #FFF");
        }
        int r = (i & 0xF00) >> 8;
        int g = (i & 0x0F0) >> 4;
        int b = i & 0x00F;

        return new Color(
                (r << 4) | r
                , (g << 4) | g
                , (b << 4) | b
        );
    }

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

}
