package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-02-07.
 *
 * 3-3-2 256 colour RGB
 */
public class Col256 {

    private byte data;

    /**
     * Create new Col256 format.
     * @param data 0x00-0xFF
     */
    public Col256(int data) {
        this.data = (byte) data;
    }

    public Col256(int r, int g, int b) {
        if (r > 7 || g > 7 || b > 3) {
            throw new IllegalArgumentException("Colour range: RG: 0-7, B:0-4");
        }

        data = (byte) (r << 5 | g << 2 | b);
    }

    /**
     * Create Col256 colour and convert it to Slick Color
     * @param i
     * @return
     */
    public Color create(int i) {
        if (i > 0xFF || i < 0) {
            throw new IllegalArgumentException("Colour range: #00 - #FF");
        }
        int r = (i & 0b11100000) >> 5;
        int g = (i & 0b00011100) >> 2;
        int b = i &  0b00000011;

        return create(r, g, b);
    }

    /**
     * Create Col256 colour and convert it to Slick Color
     * @return
     */
    public Color create(int r, int g, int b) {
        if (r > 7 || g > 7 || b > 3) {
            throw new IllegalArgumentException("Colour range: RG: 0-7, B:0-4");
        }

        int[] colIndex3 = {0, 36, 73, 109, 146, 182, 219, 255};
        int[] colIndex2 = {0, 85, 170, 255};

        return new Color(
                colIndex3[r]
                , colIndex3[g]
                , colIndex2[b]
        );
    }

    /**
     * Retrieve raw RGB value
     * @return 0bRRRGGGBB
     */
    public byte getByte() {
        return data;
    }

}
