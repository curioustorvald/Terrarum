package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.terrarum.Point2d
import net.torvald.terrarum.printStackTrace
import org.dyn4j.geometry.Vector2

/**
 * Constructor: (top-left position, width, height)
 *
 * Can also use Hitbox.fromTwoPoints(x1, y1, x2, y2
 *
 * Created by minjaesong on 2016-01-15.
 */
class Hitbox {

    var suppressWarning = true

    private constructor()

    constructor(x1: Double, y1: Double, width: Double, height: Double, suppressWarning: Boolean = true) : this() {
        this.suppressWarning = suppressWarning

        hitboxStart = Point2d(x1, y1)
        this.width = width
        this.height = height

        if (!suppressWarning && (width == 0.0 || height == 0.0)) {
            println("[Hitbox] width or height is zero ($this), perhaps you want to check it out?")
            printStackTrace(this)
        }
    }

    @Volatile var hitboxStart: Point2d = Point2d(-1.0, -1.0)
        private set
    inline val hitboxEnd: Point2d
        get() = Point2d(hitboxStart.x + width, hitboxStart.y + height)
    var width: Double = 0.0
        private set
    var height: Double = 0.0
        private set


    val startX: Double
        get() = hitboxStart.x
    val startY: Double
        get() = hitboxStart.y
    val startVec: Vector2
        get() = hitboxStart.toVector()

    val endX: Double
        get() = hitboxStart.x + width
    val endY: Double
        get() = hitboxStart.y + height
    val endVec: Vector2
        get() = hitboxEnd.toVector()

    val centeredX: Double
        get() = hitboxStart.x + width * 0.5
    val centeredY: Double
        get() = hitboxStart.y + height * 0.5
    val centerVec: Vector2
        get() = Vector2(centeredX, centeredY)

    /**
     * @return bottom-centered point of hitbox.
     */
    inline val canonicalX: Double
        get() = centeredX

    /**
     * @return bottom-centered point of hitbox.
     */
    inline val canonicalY: Double
        get() = endY

    val canonVec: Vector2
        get() = Vector2(canonicalX, canonicalY)

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

        if (!suppressWarning && (width == 0.0 || height == 0.0)) {
            println("[Hitbox] width or height is zero ($this), perhaps you want to check it out?")
            printStackTrace(this)
        }

        return this
    }
    fun setFromTwoPoints(x1: Double, y1: Double, x2: Double, y2: Double): Hitbox {
        return setFromWidthHeight(x1, y1, x2 - x1, y2 - y1)
    }
    fun reassign(other: Hitbox) = setFromTwoPoints(other.startX, other.startY, other.endX, other.endY)

    fun translate(x: Double, y: Double) = setPosition(startX + x, startY + y)
    fun translate(vec: Vector2?) = if (vec != null) translate(vec.x, vec.y) else this

    fun setPosition(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1, y1)

        if (!suppressWarning && (width == 0.0 || height == 0.0)) {
            println("[Hitbox] width or height is zero ($this), perhaps you want to check it out?")
            printStackTrace(this)
        }

        return this
    }
    fun setPosition(vector: Vector2) = setPosition(vector.x, vector.y)

    fun setPositionX(x: Double) = setPosition(x, startY)
    fun setPositionY(y: Double) = setPosition(startX, y)

    /**
     * Set position from bottom-centre point
     */
    fun setPositionFromPointed(x1: Double, y1: Double): Hitbox {
        hitboxStart = Point2d(x1 - width / 2, y1 - height)
        return this
    }

    fun translatePosX(d: Double): Hitbox {
        setPositionX(startX + d)
        return this
    }

    fun translatePosY(d: Double): Hitbox {
        setPositionY(startY + d)
        return this
    }

    /**
     * For initial setup only. Use CanonicalResize for graceful resizing
     */
    fun setDimension(w: Double, h: Double): Hitbox {
        width = w
        height = h
        return this
    }

    /**
     * Resize the hitbox centred around the "canonical" point.
     */
    fun canonicalResize(w: Double, h: Double): Hitbox {
        // sx_1 + 0.5w_1 = sx_2 + 0.5w_2 // equals because the final point must not move. sx_1: old start-x, sx_2: new start-x which is what we want
        // sx_2 = sx_1 + 0.5w_1 - 0.5w_2 // move variables to right-hand side to derive final value sx_2

        hitboxStart.set(
                startX + 0.5 * width - 0.5 * w,
                startY + height - h
        )
        width = w
        height = h
        return this
    }

    /** *ROUNDWORLD*-aware version of intersects(Double, Double). */
    fun containsPoint(worldWidth: Double, x: Double, y: Double) =
            ((x in startX..endX) || (x + worldWidth in startX..endX) || (x - worldWidth in startX..endX)) && (y in startY..endY)
    /** *ROUNDWORLD*-aware version of intersects(Poind2d). */
    fun containsPoint(worldWidth: Double, p: Point2d) = containsPoint(worldWidth, p.x, p.y)
    /** *ROUNDWORLD*-aware version of intersects(Hitbox). */
    fun containsHitbox(worldWidth: Double, other: Hitbox) =
            (
                    (this.endX >= other.startX && other.endX >= this.startX) ||
                    (this.endX >= other.startX + worldWidth && other.endX + worldWidth >= this.startX) ||
                    (this.endX >= other.startX - worldWidth && other.endX - worldWidth >= this.startX)
            ) && (this.endY >= other.startY && other.endY >= this.startY)

    fun intersects(px: Double, py: Double) =
            (px in startX..endX) &&
            (py in startY..endY)
    infix fun intersects(position: Point2d) = intersects(position.x, position.y)
    infix fun intersects(other: Hitbox) = intersects(other.startX, other.startY, other.endX, other.endY)
    fun intersects(otherStartX: Double, otherStartY: Double, otherEndX: Double, otherEndY: Double) =
            (this.endX >= otherStartX && otherEndX >= this.startX) &&
            (this.endY >= otherStartY && otherEndY >= this.startY)

    fun toVector(): Vector2 = Vector2(startX, startY)

    fun clone(): Hitbox = Hitbox(startX, startY, width, height)

    companion object {
        fun fromTwoPoints(x1: Double, y1: Double, x2: Double, y2: Double, nowarn: Boolean = false) =
                Hitbox(x1, y1, x2 - x1, y2 - y1, nowarn)

        fun lerp(fraction: Double, a: Hitbox, b: Hitbox): Hitbox {
            return Hitbox(
                FastMath.interpolateLinear(fraction, a.startX, b.startX),
                FastMath.interpolateLinear(fraction, a.startY, b.startY),
                FastMath.interpolateLinear(fraction, a.width,  b.width),
                FastMath.interpolateLinear(fraction, a.height, b.height)
            )
        }
    }

    operator fun minus(other: Hitbox): Vector2 {
        return Vector2(other.centeredX - this.centeredX, other.centeredY - this.centeredY)
    }

    override fun equals(other: Any?): Boolean {
        return this.hitboxStart == (other as Hitbox).hitboxStart &&
               this.width == other.width &&
               this.height == other.height
    }

    override fun toString(): String {
        return "[$hitboxStart - $hitboxEnd]"
    }
}
