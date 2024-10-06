package net.torvald.terrarum

import net.torvald.random.XXHash32
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2016-01-15.
 */
class Point2d() : Cloneable {

    var x: Double = 0.0
    var y: Double = 0.0

    constructor(other: Point2d) : this(other.x, other.y)

    constructor(x: Double, y: Double) : this() {
        this.x = x
        this.y = y
    }

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

    fun setCoerceIn(start: Point2d, endInclusive: Point2d) {
        x = x.coerceIn(start.x, endInclusive.x)
        y = y.coerceIn(start.y, endInclusive.y)
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

    fun toDoubleArray() = doubleArrayOf(x, y)

    fun apply(transformation: (Double, Double) -> Pair<Double, Double>): Point2d {
        val translation = transformation(x, y)
        this.x = translation.first
        this.y = translation.second

        return this
    }

    operator fun component1() = x
    operator fun component2() = y
}

class Point2i() {

    var x: Int = 0
    var y: Int = 0

    constructor(x: Int, y: Int) : this() {
        this.x = x
        this.y = y
    }

    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun set(other: Point2i) {
        this.x = other.x
        this.y = other.y
    }

    operator fun plus(other: Point2i): Point2i {
        return Point2i(this.x + other.x, this.y + other.y)
    }

    operator fun minus(other: Point2i): Point2i {
        return Point2i(other.x - this.x, other.y - this.y)
    }

    fun apply(transformation: (Int, Int) -> Pair<Int, Int>): Point2i {
        val translation = transformation(x, y)
        this.x = translation.first
        this.y = translation.second

        return this
    }

    override fun hashCode(): Int = XXHash32.hashGeoCoord(x, y)
    override fun equals(other: Any?) = if (other is Point2i)
        this.x == other.x && this.y == other.y
    else
        false

    override fun toString() = "Point2i($x, $y)"

    operator fun component1() = x
    operator fun component2() = y

    fun toVector() = Vector2(this.x.toDouble(), this.y.toDouble())
}
