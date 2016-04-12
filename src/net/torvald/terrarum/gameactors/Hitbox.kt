package net.torvald.terrarum.gameactors

import net.torvald.point.Point2f

/**
 * Created by minjaesong on 16-01-15.
 */
class Hitbox(x1: Float, y1: Float, width: Float, height: Float) {

    var hitboxStart: Point2f
        private set
    var hitboxEnd: Point2f
        private set
    var width: Float = 0.toFloat()
        private set
    var height: Float = 0.toFloat()
        private set

    init {
        hitboxStart = Point2f(x1, y1)
        hitboxEnd = Point2f(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointX
     */
    val pointedX: Float
        get() = hitboxStart.x + width / 2

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointY
     */
    val pointedY: Float
        get() = hitboxEnd.y

    /**
     * Set to the point top left
     * @param x1
     * *
     * @param y1
     * *
     * @param width
     * *
     * @param height
     */
    operator fun set(x1: Float, y1: Float, width: Float, height: Float) {
        hitboxStart = Point2f(x1, y1)
        hitboxEnd = Point2f(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    fun setPosition(x1: Float, y1: Float) {
        hitboxStart = Point2f(x1, y1)
        hitboxEnd = Point2f(x1 + width, y1 + height)
    }

    fun setPositionX(x: Float) {
        setPosition(x, posY)
    }

    fun setPositionY(y: Float) {
        setPosition(posX, y)
    }

    fun setPositionFromPoint(x1: Float, y1: Float) {
        hitboxStart = Point2f(x1 - width / 2, y1 - height)
        hitboxEnd = Point2f(hitboxStart.x + width, hitboxStart.y + height)
    }

    fun setPositionXFromPoint(x: Float) {
        setPositionFromPoint(x, pointedY)
    }

    fun setPositionYFromPoint(y: Float) {
        setPositionFromPoint(pointedX, y)
    }

    fun translatePosX(d: Float) {
        setPositionX(posX + d)
    }

    fun translatePosY(d: Float) {
        setPositionY(posY + d)
    }

    fun setDimension(w: Float, h: Float) {
        width = w
        height = h
    }

    /**
     * Returns x value of start point
     * @return top-left point posX
     */
    val posX: Float
        get() = hitboxStart.x

    /**
     * Returns y value of start point
     * @return top-left point posY
     */
    val posY: Float
        get() = hitboxStart.y

    val centeredX: Float
        get() = (hitboxStart.x + hitboxEnd.x) * 0.5f

    val centeredY: Float
        get() = (hitboxStart.y + hitboxEnd.y) * 0.5f
}
