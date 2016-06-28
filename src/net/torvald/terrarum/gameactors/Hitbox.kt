package net.torvald.terrarum.gameactors

import net.torvald.point.Point2d
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-01-15.
 */
class Hitbox(x1: Double, y1: Double, width: Double, height: Double) {

    @Volatile var hitboxStart: Point2d
        private set
    @Volatile var hitboxEnd: Point2d
        private set
    var width: Double = 0.0
        private set
    var height: Double = 0.0
        private set

    init {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    override fun toString(): String {
        return "[$hitboxStart - $hitboxEnd]"
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

    val endPointX: Double
        get() = hitboxEnd.x
    val endPointY: Double
        get() = hitboxEnd.y

    /**
     * Set to the point top left
     * @param x1
     * @param y1
     * @param width
     * @param height
     */
    fun set(x1: Double, y1: Double, width: Double, height: Double) {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        this.width = width
        this.height = height
    }

    fun reassign(other: Hitbox) {
        set(other.posX, other.posY, other.width, other.height)
    }

    fun translate(x: Double, y: Double) {
        setPosition(posX + x, posY + y)
    }

    fun translate(vec: Vector2) {
        translate(vec.x, vec.y)
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

    fun toVector(): Vector2 {
        return Vector2(posX, posY)
    }

    fun clone(): Hitbox = Hitbox(posX, posY, width, height)
}
