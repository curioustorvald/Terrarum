package com.Torvald.Terrarum.Actors;

import com.Torvald.Point.Point2f;

/**
 * Created by minjaesong on 16-01-15.
 */
public class Hitbox {

    private Point2f hitboxStart;
    private Point2f hitboxEnd;
    private float width;
    private float height;

    public Hitbox(float x1, float y1, float width, float height) {
        hitboxStart = new Point2f(x1, y1);
        hitboxEnd = new Point2f(x1 + width, y1 + height);
        this.width = width;
        this.height = height;
    }

    public Point2f getHitboxStart() {
        return hitboxStart;
    }

    public Point2f getHitboxEnd() {
        return hitboxEnd;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointX
     */
    public float getPointedX() {
        return hitboxStart.getX() + (width / 2);
    }

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointY
     */
    public float getPointedY() {
        return hitboxEnd.getY();
    }

    /**
     * Set to the point top left
     * @param x1
     * @param y1
     * @param width
     * @param height
     */
    public void set(float x1, float y1, float width, float height) {
        hitboxStart = new Point2f(x1, y1);
        hitboxEnd = new Point2f(x1 + width, y1 + height);
        this.width = width;
        this.height = height;
    }

    public void setPosition(float x1, float y1) {
        hitboxStart = new Point2f(x1, y1);
        hitboxEnd = new Point2f(x1 + width, y1 + height);
    }

    public void setPositionX(float x) {
        setPosition(x, getPosY());
    }

    public void setPositionY(float y) {
        setPosition(getPosX(), y);
    }

    public void setPositionFromPoint(float x1, float y1) {
        hitboxStart = new Point2f(x1 - (width / 2), y1 - height);
        hitboxEnd = new Point2f(hitboxStart.getX() + width, hitboxStart.getY() + height);
    }

    public void setPositionXFromPoint(float x) {
        setPositionFromPoint(x, getPointedY());
    }

    public void setPositionYFromPoint(float y) {
        setPositionFromPoint(getPointedX(), y);
    }

    public void translatePosX(float d) {
        setPositionX(getPosX() + d);
    }

    public void translatePosY(float d) {
        setPositionY(getPosY() + d);
    }

    public void setDimension(float w, float h) {
        width = w;
        height = h;
    }

    /**
     * Returns x value of start point
     * @return top-left point posX
     */
    public float getPosX() {
        return hitboxStart.getX();
    }

    /**
     * Returns y value of start point
     * @return top-left point posY
     */
    public float getPosY() {
        return hitboxStart.getY();
    }

    public float getCenteredX() {
        return (hitboxStart.getX() + hitboxEnd.getX()) * 0.5f;
    }

    public float getCenteredY() {
        return (hitboxStart.getY() + hitboxEnd.getY()) * 0.5f;
    }
}
