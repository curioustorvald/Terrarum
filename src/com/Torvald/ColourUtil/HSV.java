package com.Torvald.ColourUtil;

import com.jme3.math.FastMath;
import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-01-16.
 */
public class HSV {

    /**
     * Convert HSV parameters to RGB color.
     * @param h 0-359 Hue
     * @param s 0-255 Saturation
     * @param v 0-255 Value
     * @return org.newdawn.slick.Color
     * @link http://www.rapidtables.com/convert/color/hsv-to-rgb.htm
     */
    public static Color toRGB(int h, int s, int v) {
        int H = h;
        if (H < 0 || H >= 360) {
            H %= 360;
        }

        float S = s / 255f;
        float V = v / 255f;

        float C = V * S;
        float X = C * (1 - FastMath.abs(
                (H / 60f) % 2 - 1
        ));
        float m = V - C;

        float R_prime = Float.NaN;
        float G_prime = Float.NaN;
        float B_prime = Float.NaN;

        if (H < 60) {
            R_prime = C;
            G_prime = X;
            B_prime = 0;
        }
        else if (H < 120) {
            R_prime = X;
            G_prime = C;
            B_prime = 0;
        }
        else if (H < 180) {
            R_prime = 0;
            G_prime = C;
            B_prime = X;
        }
        else if (H < 240) {
            R_prime = 0;
            G_prime = X;
            B_prime = C;
        }
        else if (H < 300) {
            R_prime = X;
            G_prime = 0;
            B_prime = C;
        }
        else if (H < 360) {
            R_prime = C;
            G_prime = 0;
            B_prime = X;
        }

        return new Color(
                  (int) ((R_prime + m) * 255)
                , (int) ((G_prime + m) * 255)
                , (int) ((B_prime + m) * 255)
        );
    }

}
