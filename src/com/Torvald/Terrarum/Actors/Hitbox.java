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
    private float pointX;
    private float pointY;

    public Hitbox(float x1, float y1, float width, float height) {
        hitboxStart = new Point2f(x1, y1);
        hitboxEnd = new Point2f(x1 + width, y1 + height);
        this.width = width;
        this.height = height;

        pointX = x1 + (width / 2);
        pointY = y1 + height;
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
        return pointX;
    }

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointY
     */
    public float getPointedY() {
        return pointY;
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

        pointX = x1 + (width / 2);
        pointY = y1 + height;
    }

    public void setPositionFromPoint(float x1, float y1) {
        hitboxStart = new Point2f(x1 - (width / 2), y1 - height);
        hitboxEnd = new Point2f(hitboxStart.getX() + width, hitboxStart.getY() + height);
        pointX = x1;
        pointY = y1;
    }

    public void setPositionXFromPoint(float x1) {
        float y1 = pointY;
        hitboxStart = new Point2f(x1 - (width / 2), y1 - height);
        hitboxEnd = new Point2f(hitboxStart.getX() + width, hitboxStart.getY() + height);
        pointX = x1;
    }

    public void setPositionYFromPoint(float y1) {
        float x1 = pointX;
        hitboxStart = new Point2f(x1 - (width / 2), y1 - height);
        hitboxEnd = new Point2f(hitboxStart.getX() + width, hitboxStart.getY() + height);
        pointY = y1;
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
}
