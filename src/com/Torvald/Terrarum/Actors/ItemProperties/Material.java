package com.Torvald.Terrarum.Actors.ItemProperties;

/**
 * Created by minjaesong on 16-03-06.
 */
public class Material {

    /** How sharp the material is. Default to 1000.*/
    int maxEdge;
    /** Self-explanatory. [kPa in Vickers hardness]*/
    int hardness;
    /** Self-explanatory. [g/l]*/
    int density;

    public Material() {
    }

    public int getMaxEdge() {
        return maxEdge;
    }

    void setMaxEdge(int maxEdge) {
        this.maxEdge = maxEdge;
    }

    public int getHardness() {
        return hardness;
    }

    void setHardness(int hardness) {
        this.hardness = hardness;
    }

    public int getDensity() {
        return density;
    }

    void setDensity(int density) {
        this.density = density;
    }
}
