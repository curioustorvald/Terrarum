package com.Torvald.ColourUtil;

/**
 * Created by minjaesong on 16-03-10.
 */
public class HSV {
    private float H;
    private float S;
    private float V;

    public HSV() {
    }

    /**
     *
     * @param h 0-359
     * @param s 0-1
     * @param v 0-1
     */
    public HSV(float h, float s, float v) {
        H = h;
        S = s;
        V = v;
    }

    public float getH() {
        return H;
    }

    public void setH(float h) {
        H = h;
    }

    public float getS() {
        return S;
    }

    public void setS(float s) {
        S = s;
    }

    public float getV() {
        return V;
    }

    public void setV(float v) {
        V = v;
    }
}
