package net.torvald.terrarum.gameactors

import net.torvald.point.Point2d
import org.dyn4j.geometry.Vector2

/**
 * Constructor: (top-left position, width, height)
 *
 * Can also use Hitbox.fromTwoPoints(x1, y1, x2, y2
 *
 * Created by minjaesong on 16-01-15.
 */
class Hitbox(x1: Double, y1: Double, width: Double, height: Double) {

    @Volatile var hitboxStart: Point2d
        private set
    val hitboxEnd: Point2d
        get() = Point2d(hitboxStart.x + width, hitboxStart.y + height)
    var width: Double = 0.0
        private set
    var height: Double = 0.0
        private set

    init {
        hitboxStart = Point2d(x1, y1)
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
        get() = centeredX

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
    fun setFromWidthHeight(x1: Double, y1: Double, width: Double, height: Double): Hitbox {
        hitboxStart = Point2d(x1, y1)
        this.width = width
        this.height = height
        return this
    }
    fun setFromTwoPoints(x1: Double, y1: Double, x2: Double, y2: Double): Hitbox {
        return setFromWidthHeight(x1, y1, x2 - x1, y2 - y1)
    }
    fun reassign(other: Hitbox) = setFromTwoPoints(other.posX, other.posY, other.endPointX, other.endPointY)

    fun translate(x: Double, y: Double) = setPosition(posX + x, posY + y)
    fun translate(vec: Vector2?) = if (vec != null) translate(vec.x, vec.y) else this

    fun setPosition(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1, y1)
        return this
    }
    fun setPosition(vector: Vector2) = setPosition(vector.x, vector.y)

    fun setPositionX(x: Double) = setPosition(x, posY)
    fun setPositionY(y: Double) = setPosition(posX, y)

    fun setPositionFromPointed(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1 - width / 2, y1 - height)
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
        get() = (hitboxStart.x + hitboxEnd.x) * 0.5

    val centeredY: Double
        get() = (hitboxStart.y + hitboxEnd.y) * 0.5

    fun intersects(position: Point2d) =
            (position.x >= posX && position.x <= posX + width) &&
            (position.y >= posY && position.y <= posY + height)

    fun toVector(): Vector2 = Vector2(posX, posY)

    fun clone(): Hitbox = Hitbox(posX, posY, width, height)

    companion object {
        fun fromTwoPoints(x1: Double, y1: Double, x2: Double, y2: Double) =
                Hitbox(x1, y1, x2 - x1, y2 - y1)
    }

    operator fun minus(other: Hitbox): Vector2 {
        return Vector2(other.centeredX - this.centeredX, other.centeredY - this.centeredY)
    }
}
