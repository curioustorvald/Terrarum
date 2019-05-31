/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package net.torvald.gdx.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.NumberUtils;

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner */
public class Cvec {
    public static final Cvec WHITE = new Cvec(1, 1, 1,1);

    /** the red, green, blue and alpha components **/
    public float r, g, b, a;

    /** Constructs a new Cvec with all components set to 0. */
    public Cvec () {
    }

    /** @see #rgba8888ToCvec(Cvec, int) */
    public Cvec (int rgba8888) {
        rgba8888ToCvec(this, rgba8888);
    }

    public Cvec (Color color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
    }

    /** Constructor, sets the components of the color
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component */
    public Cvec (float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /** Constructs a new color using the given color
     *
     * @param color the color */
    public Cvec (Cvec color) {
        set(color);
    }

    /** Sets this color to the given color.
     *
     * @param color the Cvec */
    public Cvec set (Cvec color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        return this;
    }

    /** Multiplies the this color and the given color
     *
     * @param color the color
     * @return this color. */
    public Cvec mul (Cvec color) {
        this.r *= color.r;
        this.g *= color.g;
        this.b *= color.b;
        this.a *= color.a;
        return this;
    }

    /** Multiplies all components of this Cvec with the given value.
     *
     * @param value the value
     * @return this color */
    public Cvec mul (float value) {
        this.r *= value;
        this.g *= value;
        this.b *= value;
        this.a *= value;
        return this;
    }

    /** Adds the given color to this color.
     *
     * @param color the color
     * @return this color */
    public Cvec add (Cvec color) {
        this.r += color.r;
        this.g += color.g;
        this.b += color.b;
        this.a += color.a;
        return this;
    }

    /** Subtracts the given color from this color
     *
     * @param color the color
     * @return this color */
    public Cvec sub (Cvec color) {
        this.r -= color.r;
        this.g -= color.g;
        this.b -= color.b;
        this.a -= color.a;
        return this;
    }

    /** Sets this Cvec's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining */
    public Cvec set (float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    /** Sets this color's component values through an integer representation.
     *
     * @return this Cvec for chaining
     * @see #rgba8888ToCvec(Cvec, int) */
    public Cvec set (int rgba) {
        rgba8888ToCvec(this, rgba);
        return this;
    }

    /** Adds the given color component values to this Cvec's values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining */
    public Cvec add (float r, float g, float b, float a) {
        this.r += r;
        this.g += g;
        this.b += b;
        this.a += a;
        return this;
    }

    /** Subtracts the given values from this Cvec's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining */
    public Cvec sub (float r, float g, float b, float a) {
        this.r -= r;
        this.g -= g;
        this.b -= b;
        this.a -= a;
        return this;
    }

    /** Multiplies this Cvec's color components by the given ones.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Cvec for chaining */
    public Cvec mul (float r, float g, float b, float a) {
        this.r *= r;
        this.g *= g;
        this.b *= b;
        this.a *= a;
        return this;
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param target The target color
     * @param t The interpolation coefficient
     * @return This color for chaining. */
    public Cvec lerp (final Cvec target, final float t) {
        this.r += t * (target.r - this.r);
        this.g += t * (target.g - this.g);
        this.b += t * (target.b - this.b);
        this.a += t * (target.a - this.a);
        return this;
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param r The red component of the target color
     * @param g The green component of the target color
     * @param b The blue component of the target color
     * @param a The alpha component of the target color
     * @param t The interpolation coefficient
     * @return This color for chaining. */
    public Cvec lerp (final float r, final float g, final float b, final float a, final float t) {
        this.r += t * (r - this.r);
        this.g += t * (g - this.g);
        this.b += t * (b - this.b);
        this.a += t * (a - this.a);
        return this;
    }

    /** Multiplies the RGB values by the alpha. */
    public Cvec premultiplyAlpha () {
        r *= a;
        g *= a;
        b *= a;
        return this;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cvec color = (Cvec)o;
        return toIntBits() == color.toIntBits();
    }

    @Override
    public int hashCode () {
        int result = (r != +0.0f ? NumberUtils.floatToIntBits(r) : 0);
        result = 31 * result + (g != +0.0f ? NumberUtils.floatToIntBits(g) : 0);
        result = 31 * result + (b != +0.0f ? NumberUtils.floatToIntBits(b) : 0);
        result = 31 * result + (a != +0.0f ? NumberUtils.floatToIntBits(a) : 0);
        return result;
    }

    /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see {@link NumberUtils#intToFloatColor(int)}).
     * @return the packed color as a 32-bit float */
    public float toFloatBits () {
        int color = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return NumberUtils.intToFloatColor(color);
    }

    /** Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int. */
    public int toIntBits () {
        return ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA. */
    public String toString () {
        String value = Integer
                .toHexString(((int)(255 * r) << 24) | ((int)(255 * g) << 16) | ((int)(255 * b) << 8) | ((int)(255 * a)));
        while (value.length() < 8)
            value = "0" + value;
        return value;
    }

    /** Returns a new color from a hex string with the format RRGGBBAA.
     * @see #toString() */
    public static Cvec valueOf (String hex) {
        hex = hex.charAt(0) == '#' ? hex.substring(1) : hex;
        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);
        int a = hex.length() != 8 ? 255 : Integer.valueOf(hex.substring(6, 8), 16);
        return new Cvec(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /** Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
     * performance.
     * @param r the red component, 0 - 255
     * @param g the green component, 0 - 255
     * @param b the blue component, 0 - 255
     * @param a the alpha component, 0 - 255
     * @return the packed color as a 32-bit int */
    public static int toIntBits (int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static int alpha (float alpha) {
        return (int)(alpha * 255.0f);
    }

    public static int rgba8888 (float r, float g, float b, float a) {
        return ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | ((int)(b * 255) << 8) | (int)(a * 255);
    }

    public static int argb8888 (float a, float r, float g, float b) {
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public static int rgba8888 (Cvec color) {
        return ((int)(color.r * 255) << 24) | ((int)(color.g * 255) << 16) | ((int)(color.b * 255) << 8) | (int)(color.a * 255);
    }

    public static int argb8888 (Cvec color) {
        return ((int)(color.a * 255) << 24) | ((int)(color.r * 255) << 16) | ((int)(color.g * 255) << 8) | (int)(color.b * 255);
    }

    /** Sets the Cvec components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
     * b, a) method.
     *
     * @param color The Cvec to be modified.
     * @param value An integer color value in RGBA8888 format. */
    public static void rgba8888ToCvec (Cvec color, int value) {
        color.r = ((value & 0xff000000) >>> 24) / 255f;
        color.g = ((value & 0x00ff0000) >>> 16) / 255f;
        color.b = ((value & 0x0000ff00) >>> 8) / 255f;
        color.a = ((value & 0x000000ff)) / 255f;
    }

    /** Sets the Cvec components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
     * r, g, b) method
     *
     * @param color The Cvec to be modified.
     * @param value An integer color value in ARGB8888 format. */
    public static void argb8888ToCvec (Cvec color, int value) {
        color.a = ((value & 0xff000000) >>> 24) / 255f;
        color.r = ((value & 0x00ff0000) >>> 16) / 255f;
        color.g = ((value & 0x0000ff00) >>> 8) / 255f;
        color.b = ((value & 0x000000ff)) / 255f;
    }

    /** Sets the Cvec components using the specified float value in the format ABGB8888.
     * @param color The Cvec to be modified. */
    public static void abgr8888ToCvec (Cvec color, float value) {
        int c = NumberUtils.floatToIntColor(value);
        color.a = ((c & 0xff000000) >>> 24) / 255f;
        color.b = ((c & 0x00ff0000) >>> 16) / 255f;
        color.g = ((c & 0x0000ff00) >>> 8) / 255f;
        color.r = ((c & 0x000000ff)) / 255f;
    }

    /** Sets the RGB Cvec components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
     * to preserve high range color and can range beyond typical values.
     * @param h The Hue in degree from 0 to 360
     * @param s The Saturation from 0 to 1
     * @param v The Value (brightness) from 0 to 1
     * @return The modified Cvec for chaining. */
    public Cvec fromHsv (float h, float s, float v) {
        float x = (h / 60f + 6) % 6;
        int i = (int)x;
        float f = x - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        switch (i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
        }

        //return clamp();
        return this;
    }

    /** Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
     * {@link #fromHsv(float, float, float)}. This is the inverse of {@link #toHsv(float[])}.
     * @param hsv The Hue, Saturation and Value components in that order.
     * @return The modified Cvec for chaining. */
    public Cvec fromHsv (float[] hsv) {
        return fromHsv(hsv[0], hsv[1], hsv[2]);
    }

    /** Extract Hue-Saturation-Value. This is the inverse of {@link #fromHsv(float[])}.
     * @param hsv The HSV array to be modified.
     * @return HSV components for chaining. */
    public float[] toHsv (float[] hsv) {
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float range = max - min;
        if (range == 0) {
            hsv[0] = 0;
        } else if (max == r) {
            hsv[0] = (60 * (g - b) / range + 360) % 360;
        } else if (max == g) {
            hsv[0] = 60 * (b - r) / range + 120;
        } else {
            hsv[0] = 60 * (r - g) / range + 240;
        }

        if (max > 0) {
            hsv[1] = 1 - min / max;
        } else {
            hsv[1] = 0;
        }

        hsv[2] = max;

        return hsv;
    }

    /** @return a copy of this color */
    public Cvec cpy () {
        return new Cvec(this);
    }
}
