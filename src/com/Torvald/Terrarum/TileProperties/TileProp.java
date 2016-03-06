package com.Torvald.Terrarum.TileProperties;

/**
 * Created by minjaesong on 16-02-16.
 */
public class TileProp {

    private int id;
    private int damage;
    private String name;

    private char opacity; // colour attenuation

    private int strength;

    private boolean fluid;
    private int viscocity;

    private boolean solid; // transparent or not

    private boolean wallable;

    private boolean opaque; // hides wall or not

    private char luminosity;

    private int drop;
    private int dropDamage;

    private boolean fallable;

    private int friction;

    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public char getOpacity() {
        return opacity;
    }

    void setOpacity(char opacity) {
        this.opacity = opacity;
    }

    public int getStrength() {
        return strength;
    }

    void setStrength(int strength) {
        this.strength = strength;
    }

    public boolean isFluid() {
        return fluid;
    }

    void setFluid(boolean fluid) {
        this.fluid = fluid;
    }

    public int getViscocity() {
        return viscocity;
    }

    void setViscocity(int viscocity) {
        this.viscocity = viscocity;
    }

    public boolean isSolid() {
        return solid;
    }

    void setSolid(boolean solid) {
        this.solid = solid;
    }

    public boolean isWallable() {
        return wallable;
    }

    void setWallable(boolean wallable) {
        this.wallable = wallable;
    }

    /**
     * Raw RGB value, without alpha
     * @return
     */
    public char getLuminosity() {
        return luminosity;
    }

    /**
     *
     * @param luminosity Raw RGB value, without alpha
     */
    void setLuminosity(char luminosity) {
        this.luminosity = luminosity;
    }

    public int getDrop() {
        return drop;
    }

    void setDrop(int drop) {
        this.drop = drop;
    }

    public int getDropDamage() {
        return dropDamage;
    }

    public void setDropDamage(int dropDamage) {
        this.dropDamage = dropDamage;
    }

    public boolean isFallable() {
        return fallable;
    }

    void setFallable(boolean fallable) {
        this.fallable = fallable;
    }

    public int getFriction() {
        return friction;
    }

    void setFriction(int friction) {
        this.friction = friction;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }
}
