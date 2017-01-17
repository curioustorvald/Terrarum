package net.torvald.point

import net.torvald.terrarum.gameactors.sqr
import net.torvald.terrarum.gameactors.sqrt
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-01-15.
 */
class Point2d(var x: Double, var y: Double) : Cloneable {

    override fun toString(): String {
        return "($x, $y)"
    }

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun set(other: Point2d) {
        this.x = other.x
        this.y = other.y
    }

    /**
     * Rotate transform this point, with pivot (0, 0)
     * @return new Point2d that is rotated
     */
    infix fun rot(deg_delta: Double) = Point2d(
            x * Math.cos(deg_delta) - y * Math.sin(deg_delta),
            x * Math.sin(deg_delta) + y * Math.cos(deg_delta)
    )

    fun translate(other: Point2d) {
        x += other.x
        y += other.y
    }
    fun translate(tx: Double, ty: Double) {
        x += tx
        y += ty
    }
    operator fun plus(other: Point2d) = Point2d(x + other.x, y + other.y)
    operator fun minus(other: Point2d) = Point2d(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Point2d(x * scalar, y * scalar)
    operator fun times(other: Point2d) = Point2d(x * other.x, y * other.y)
    operator fun div(scalar: Double) = Point2d(x / scalar, y / scalar)
    operator fun div(other: Point2d) = Point2d(x / other.x, y / other.y)

    fun toVector() = Vector2(x, y)

    fun copy() = Point2d(x, y)

    fun length(other: Point2d) = distSqr(other).sqrt()
    fun distSqr(other: Point2d) = ((this.x - other.x).sqr() + (this.y - other.y).sqr())
}
