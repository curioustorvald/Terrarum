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

    val HALF_PIXEL = 0.5

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
    fun set(x1: Double, y1: Double, width: Double, height: Double): Hitbox {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        this.width = width
        this.height = height
        return this
    }
    fun reassign(other: Hitbox) = set(other.posX, other.posY, other.width, other.height)

    fun translate(x: Double, y: Double) = setPosition(posX + x, posY + y)
    fun translate(vec: Vector2) = translate(vec.x, vec.y)

    fun setPosition(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1, y1)
        hitboxEnd = Point2d(x1 + width, y1 + height)
        return this
    }
    fun setPosition(vector: Vector2) = setPosition(vector.x, vector.y)

    fun setPositionX(x: Double) = setPosition(x, posY)
    fun setPositionY(y: Double) = setPosition(posX, y)

    fun setPositionFromPoint(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1 - width / 2, y1 - height)
        hitboxEnd = Point2d(hitboxStart.x + width, hitboxStart.y + height)
        return this
    }

    fun translatePosX(d: Double): Hitbox {
        setPositionX(posX + d)
        return this
    }

    fun translatePosY(d: Double): Hitbox {
        setPositionY(posY + d)
        return this
    }

    fun setDimension(w: Double, h: Double): Hitbox {
        width = w
        height = h
        return this
    }

    fun snapToPixel(): Hitbox {
        hitboxStart.x = Math.round(hitboxStart.x - HALF_PIXEL).toDouble()
        hitboxStart.y = Math.round(hitboxStart.y - HALF_PIXEL).toDouble()
        hitboxEnd.x = Math.round(hitboxEnd.x - HALF_PIXEL).toDouble()
        hitboxEnd.y = Math.round(hitboxEnd.y - HALF_PIXEL).toDouble()
        return this
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

    fun toVector(): Vector2 = Vector2(posX, posY)

    fun clone(): Hitbox = Hitbox(posX, posY, width, height)
}
