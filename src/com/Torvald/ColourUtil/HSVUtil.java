package com.Torvald.ColourUtil;

import com.jme3.math.FastMath;
import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-01-16.
 */
public class HSVUtil {

    /**
     * Convert HSV parameters to RGB color.
     * @param H 0-359 Hue
     * @param S 0-1 Saturation
     * @param V 0-1 Value
     * @return org.newdawn.slick.Color
     * @link http://www.rapidtables.com/convert/color/hsv-to-rgb.htm
     */
    public static Color toRGB(float H, float S, float V) {
        H %= 360;

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

    public static Color toRGB(HSV hsv) {
        return toRGB(hsv.getH(), hsv.getS(), hsv.getV());
    }

    public static HSV fromRGB(Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float rgbMin = FastMath.min(r, g, b);
        float rgbMax = FastMath.max(r, g, b);

        float h;
        float s;
        float v = rgbMax;

        float delta = rgbMax - rgbMin;

        if (rgbMax != 0)
            s = delta / rgbMax;
        else {
            h = 0;
            s = 0;
            return new HSV(h, s, v);
        }

        if (r == rgbMax)
            h = (g - b) / delta;
        else if (g == rgbMax)
            h = 2 + (b - r) / delta;
        else
            h = 4 + (r - g) / delta;

        h *= 60;
        if (h < 0) h += 360;

        return new HSV(h, s, v);
    }

}
