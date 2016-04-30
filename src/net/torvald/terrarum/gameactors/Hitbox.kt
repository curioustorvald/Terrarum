package net.torvald.terrarum.gameactors

import net.torvald.point.Point2d

/**
 * Created by minjaesong on 16-01-15.
 */
class Hitbox(x1: Double, y1: Double, width: Double, height: Double) {

    @Volatile var hitboxStart: Point2d
        private set
    @Volatile var hitboxEnd: Point2d
        private set
    var width: Double = 0.toDouble()
        private set
    var height: Double = 0.toDouble()
        private set

    init {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointX
     */
    val pointedX: Double
        get() = hitboxStart.x + width / 2

    /**
     * Returns bottom-centered point of hitbox.
     * @return pointY
     */
    val pointedY: Double
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
    operator fun set(x1: Double, y1: Double, width: Double, height: Double) {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    fun setPosition(x1: Double, y1: Double) {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
    }

    fun setPositionX(x: Double) {
        setPosition(x, posY)
    }

    fun setPositionY(y: Double) {
        setPosition(posX, y)
    }

    fun setPositionFromPoint(x1: Double, y1: Double) {
        hitboxStart = Point2d(x1 - width / 2, y1 - height)
        hitboxEnd = Point2d(hitboxStart.x + width, hitboxStart.y + height)
    }

    fun setPositionXFromPoint(x: Double) {
        setPositionFromPoint(x, pointedY)
    }

    fun setPositionYFromPoint(y: Double) {
        setPositionFromPoint(pointedX, y)
    }

    fun translatePosX(d: Double) {
        setPositionX(posX + d)
    }

    fun translatePosY(d: Double) {
        setPositionY(posY + d)
    }

    fun setDimension(w: Double, h: Double) {
        width = w
        height = h
    }

    /**
     * Returns x value of start point
     * @return top-left point posX
     */
    val posX: Double
        get() = hitboxStart.x

    /**
     * Returns y value of start point
     * @return top-left point posY
     */
    val posY: Double
        get() = hitboxStart.y

    val centeredX: Double
        get() = (hitboxStart.x + hitboxEnd.x) * 0.5f

    val centeredY: Double
        get() = (hitboxStart.y + hitboxEnd.y) * 0.5f
}
