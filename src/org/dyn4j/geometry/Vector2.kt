/*
 * Copyright (c) 2010-2015 William Bittle  http://www.dyn4j.org/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions
 *     and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *     and the following disclaimer in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of dyn4j nor the names of its contributors may be used to endorse or
 *     promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Kotlin translated and modified code Copyright (c) 2016 Minjaesong (Torvald).
 */
package org.dyn4j.geometry

import org.dyn4j.Epsilon

/**
 * This class represents a vector or point in 2D space.
 *
 *
 * The operations [Vector2.setMagnitude], [Vector2.getNormalized],
 * [Vector2.project], and [Vector2.normalize] require the [Vector2]
 * to be non-zero in length.
 *
 * ===============================================================================
 *
 * In this Kotlin code, you can use regular operators like + - * /.
 *
 * |operator   |function             |
 * |-----------|---------------------|
 * |a + b      |Vector2(a).plus(b)   |
 * |a - b      |Vector2(a).minus(b)  |
 * |a * b      |Vector2(a).times(b)  |
 * |a / b      |Vector2(a).div(b)    |
 * |a += b     |a.plusAssign(b)      |
 * |a -= b     |a.minusAssign(b)     |
 * |a *= b     |a.timesAssign(b)     |
 * |a /= b     |a.divAssign(b)       |
 * |a dot b    |Vector2(a).dot(b)    |
 * |a cross b  |Vector2(a).cross(b)  |
 * |!a         |this.negative        |
 * |a rotate th|Vector2(a).rotate(th)|
 * |a to b     |Vector2(a).to(b)     |
 *
 * @author William Bittle
 * *
 * @version 3.1.11
 * *
 * @since 1.0.0
 */
class Vector2 {

    /** The magnitude of the x component of this [Vector2]  */
    @Volatile var x: Double = 0.0

    /** The magnitude of the y component of this [Vector2]  */
    @Volatile var y: Double = 0.0

    /** Default constructor.  */
    constructor() {
    }

    /**
     * Copy constructor.
     * @param vector the [Vector2] to copy from
     */
    constructor(vector: Vector2) {
        this.x = vector.x
        this.y = vector.y
    }

    /**
     * Optional constructor.
     * @param x the x component
     * *
     * @param y the y component
     */
    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    /**
     * Creates a [Vector2] from the first point to the second point.
     * @param x1 the x coordinate of the first point
     * *
     * @param y1 the y coordinate of the first point
     * *
     * @param x2 the x coordinate of the second point
     * *
     * @param y2 the y coordinate of the second point
     */
    constructor(x1: Double, y1: Double, x2: Double, y2: Double) {
        this.x = x2 - x1
        this.y = y2 - y1
    }

    /**
     * Creates a [Vector2] from the first point to the second point.
     * @param p1 the first point
     * *
     * @param p2 the second point
     */
    constructor(p1: Vector2, p2: Vector2) {
        this.x = p2.x - p1.x
        this.y = p2.y - p1.y
    }

    /**
     * Creates a unit length vector in the given direction.
     * @param direction the direction in radians
     * *
     * @since 3.0.1
     */
    constructor(direction: Double) {
        this.x = Math.cos(direction)
        this.y = Math.sin(direction)
    }

    /**
     * Returns a copy of this [Vector2].
     * @return [Vector2]
     */
    fun copy(): Vector2 {
        return Vector2(this.x, this.y)
    }

    /**
     * Returns the distance from this point to the given point.
     * @param x the x coordinate of the point
     * *
     * @param y the y coordinate of the point
     * *
     * @return double
     */
    fun distance(x: Double, y: Double): Double {
        //return Math.hypot(this.x - x, this.y - y);
        val dx = this.x - x
        val dy = this.y - y
        return Math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Returns the distance from this point to the given point.
     * @param point the point
     * *
     * @return double
     */
    fun distance(point: Vector2): Double {
        //return Math.hypot(this.x - point.x, this.y - point.y);
        val dx = this.x - point.x
        val dy = this.y - point.y
        return Math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Returns the distance from this point to the given point squared.
     * @param x the x coordinate of the point
     * *
     * @param y the y coordinate of the point
     * *
     * @return double
     */
    fun distanceSquared(x: Double, y: Double): Double {
        //return (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y);
        val dx = this.x - x
        val dy = this.y - y
        return dx * dx + dy * dy
    }

    /**
     * Returns the distance from this point to the given point squared.
     * @param point the point
     * *
     * @return double
     */
    fun distanceSquared(point: Vector2): Double {
        //return (this.x - point.x) * (this.x - point.x) + (this.y - point.y) * (this.y - point.y);
        val dx = this.x - point.x
        val dy = this.y - point.y
        return dx * dx + dy * dy
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(x)
        result = prime * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(y)
        result = prime * result + (temp xor temp.ushr(32)).toInt()
        return result
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj === this) return true
        if (obj is Vector2) {
            return this.x == obj.x && this.y == obj.y
        }
        return false
    }

    /**
     * Returns true if the x and y components of this [Vector2]
     * are the same as the given [Vector2].
     * @param vector the [Vector2] to compare to
     * *
     * @return boolean
     */
    fun equals(vector: Vector2?): Boolean {
        if (vector == null) return false
        if (this === vector) {
            return true
        }
        else {
            return this.x == vector.x && this.y == vector.y
        }
    }

    /**
     * Returns true if the x and y components of this [Vector2]
     * are the same as the given x and y components.
     * @param x the x coordinate of the [Vector2] to compare to
     * *
     * @param y the y coordinate of the [Vector2] to compare to
     * *
     * @return boolean
     */
    fun equals(x: Double, y: Double): Boolean {
        return this.x == x && this.y == y
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("(").append(this.x).append(", ").append(this.y).append(")")
        return sb.toString()
    }

    /**
     * Sets this [Vector2] to the given [Vector2].
     * @param vector the [Vector2] to set this [Vector2] to
     * *
     * @return [Vector2] this vector
     */
    fun set(vector: Vector2) {
        this.x = vector.x
        this.y = vector.y
    }

    /**
     * Sets this [Vector2] to the given [Vector2].
     * @param x the x component of the [Vector2] to set this [Vector2] to
     * *
     * @param y the y component of the [Vector2] to set this [Vector2] to
     * *
     * @return [Vector2] this vector
     */
    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    /**
     * Returns the x component of this [Vector2].
     * @return [Vector2]
     */
    val xComponent: Vector2
        get() = Vector2(this.x, 0.0)

    /**
     * Returns the y component of this [Vector2].
     * @return [Vector2]
     */
    val yComponent: Vector2
        get() = Vector2(0.0, this.y)

    /**
     * Returns the magnitude of this [Vector2].
     * @return double
     */
    // the magnitude is just the pathagorean theorem
    val magnitude: Double
        get() = Math.sqrt(this.x * this.x + this.y * this.y)

    /**
     * Returns the magnitude of this [Vector2] squared.
     * @return double
     */
    val magnitudeSquared: Double
        get() = this.x * this.x + this.y * this.y

    /**
     * Sets the magnitude of the [Vector2].
     * @param magnitude the magnitude
     * *
     * @return [Vector2] this vector
     */
    fun setMagnitude(magnitude: Double): Vector2 {
        // check the given magnitude
        if (Math.abs(magnitude) <= Epsilon.E) {
            return Vector2(0.0, 0.0)
        }
        // is this vector a zero vector?
        if (this.isZero) {
            return Vector2(0.0, 0.0)
        }
        // get the magnitude
        var mag = Math.sqrt(this.x * this.x + this.y * this.y)
        // normalize and multiply by the new magnitude
        mag = magnitude / mag
        val newX = this.x * mag
        val newY = this.y * mag
        return Vector2(newX, newY)
    }

    /**
     * Returns the direction of this [Vector2]
     * as an angle in radians.
     * @return double angle in radians [-, ]
     */
    val direction: Double
        get() = Math.atan2(this.y, this.x)

    /**
     * Sets the direction of this [Vector2].
     * @param angle angle in radians
     * *
     * @return [Vector2] this vector
     */
    fun setDirection(angle: Double): Vector2 {
        //double magnitude = Math.hypot(this.x, this.y);
        val magnitude = Math.sqrt(this.x * this.x + this.y * this.y)
        val newX = magnitude * Math.cos(angle)
        val newY = magnitude * Math.sin(angle)
        return Vector2(newX, newY)
    }

    /**
     * Adds the given [Vector2] to this [Vector2].
     * @param vector the [Vector2]
     * *
     * @return [Vector2] this vector
     */
    operator fun plus(vector: Vector2): Vector2 {
        return Vector2(this.x + vector.x, this.y + vector.y)
    }

    operator fun plusAssign(vector: Vector2) {
        this.x += vector.x
        this.y += vector.y
    }

    /**
     * Subtracts the given [Vector2] from this [Vector2].
     * @param vector the [Vector2]
     * *
     * @return [Vector2] this vector
     */
    operator fun minus(vector: Vector2): Vector2 {
        return Vector2(this.x - vector.x, this.y - vector.y)
    }

    operator fun minusAssign(vector: Vector2) {
        this.x -= vector.x
        this.y -= vector.y
    }

    /**
     * Creates a [Vector2] from this [Vector2] to the given [Vector2].
     * @param other : the other [Vector2]
     * *
     * @return [Vector2]
     */
    infix fun to(other: Vector2): Vector2 {
        return Vector2(other.x - this.x, other.y - this.y)
    }

    /**
     * Get product of this vector.
     * @param scalar the scalar
     * *
     * @return [Vector2] this vector
     */
    operator fun times(scalar: Double): Vector2 {
        return product(scalar)
    }

    operator fun timesAssign(scalar: Double) {
        this.x *= scalar
        this.y *= scalar
    }

    /**
     * Multiplies this [Vector2] by the given scalar.
     * @param scalar the scalar
     * *
     * @return [Vector2] this vector
     */
    operator fun div(scalar: Double): Vector2 {
        return Vector2(this.x / scalar, this.y / scalar)
    }

    operator fun divAssign(scalar: Double) {
        this.x /= scalar
        this.y /= scalar
    }

    /**
     * Multiplies this [Vector2] by the given scalar returning
     * a new [Vector2] containing the result.
     * @param scalar the scalar
     * *
     * @return [Vector2]
     */
    infix fun product(scalar: Double): Vector2 {
        return Vector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Returns the dot product of the given [Vector2]
     * and this [Vector2].
     * @param vector the [Vector2]
     * *
     * @return double
     */
    infix fun dot(vector: Vector2): Double {
        return this.x * vector.x + this.y * vector.y
    }

    /**
     * Returns the dot product of the given [Vector2]
     * and this [Vector2].
     * @param x the x component of the [Vector2]
     * *
     * @param y the y component of the [Vector2]
     * *
     * @return double
     */
    fun dot(x: Double, y: Double): Double {
        return this.x * x + this.y * y
    }

    /**
     * Returns the cross product of the this [Vector2] and the given [Vector2].
     * @param vector the [Vector2]
     * *
     * @return double
     */
    infix fun cross(vector: Vector2): Double {
        return this.x * vector.y - this.y * vector.x
    }

    /**
     * Returns the cross product of the this [Vector2] and the given [Vector2].
     * @param x the x component of the [Vector2]
     * *
     * @param y the y component of the [Vector2]
     * *
     * @return double
     */
    fun cross(x: Double, y: Double): Double {
        return this.x * y - this.y * x
    }

    /**
     * Returns the cross product of this [Vector2] and the z value of the right [Vector2].
     * @param z the z component of the [Vector2]
     * *
     * @return [Vector2]
     */
    infix fun cross(z: Double): Vector2 {
        return Vector2(-1.0 * this.y * z, this.x * z)
    }

    /**
     * Returns true if the given [Vector2] is orthogonal (perpendicular)
     * to this [Vector2].
     *
     *
     * If the dot product of this vector and the given vector is
     * zero then we know that they are perpendicular
     * @param vector the [Vector2]
     * *
     * @return boolean
     */
    fun isOrthogonal(vector: Vector2): Boolean {
        return if (Math.abs(this.x * vector.x + this.y * vector.y) <= Epsilon.E) true else false
    }

    /**
     * Returns true if the given [Vector2] is orthogonal (perpendicular)
     * to this [Vector2].
     *
     *
     * If the dot product of this vector and the given vector is
     * zero then we know that they are perpendicular
     * @param x the x component of the [Vector2]
     * *
     * @param y the y component of the [Vector2]
     * *
     * @return boolean
     */
    fun isOrthogonal(x: Double, y: Double): Boolean {
        return if (Math.abs(this.x * x + this.y * y) <= Epsilon.E) true else false
    }

    /**
     * Returns true if this [Vector2] is the zero [Vector2].
     * @return boolean
     */
    val isZero: Boolean
        get() = Math.abs(this.x) <= Epsilon.E && Math.abs(this.y) <= Epsilon.E

    /**
     * Negates this [Vector2].
     * @return [Vector2] this vector
     */
    operator fun not() = this.negative

    /**
     * Negates this [Vector2].
     * @return [Vector2] this vector
     */
    operator fun unaryMinus() = this.negative

    /**
     * Returns a [Vector2] which is the negative of this [Vector2].
     * @return [Vector2]
     */
    val negative: Vector2
        get() = Vector2(-this.x, -this.y)

    /**
     * Sets the [Vector2] to the zero [Vector2]
     * @return [Vector2] this vector
     */
    fun zero() {
        this.x = 0.0
        this.y = 0.0
    }

    /**
     * Rotates about the origin.
     * @param theta the rotation angle in radians
     * *
     * @return [Vector2] this vector
     */
    infix fun rotate(theta: Double): Vector2 {
        val cos = Math.cos(theta)
        val sin = Math.sin(theta)
        val x = this.x
        val y = this.y
        val newX = x * cos - y * sin
        val newY = x * sin + y * cos
        return Vector2(newX, newY)
    }

    /**
     * Projects this [Vector2] onto the given [Vector2].
     * @param vector the [Vector2]
     * *
     * @return [Vector2] the projected [Vector2]
     */
    fun project(vector: Vector2): Vector2 {
        val dotProd = this.dot(vector)
        var denominator = vector.dot(vector)
        if (denominator <= Epsilon.E) return Vector2()
        denominator = dotProd / denominator
        return Vector2(denominator * vector.x, denominator * vector.y)
    }

    /**
     * Returns the right-handed normal of this vector.
     * @return [Vector2] the right hand orthogonal [Vector2]
     */
    val rightHandOrthogonalVector: Vector2
        get() = Vector2(-this.y, this.x)

    /**
     * Sets this vector to the right-handed normal of this vector.
     * @return [Vector2] this vector
     * *
     * @see .getRightHandOrthogonalVector
     */
    fun right(): Vector2 {
        val temp = this.x
        val newX = -this.y
        val newY = temp
        return Vector2(newX, newY)
    }

    /**
     * Returns the left-handed normal of this vector.
     * @return [Vector2] the left hand orthogonal [Vector2]
     */
    val leftHandOrthogonalVector: Vector2
        get() = Vector2(this.y, -this.x)

    /**
     * Sets this vector to the left-handed normal of this vector.
     * @return [Vector2] this vector
     * *
     * @see .getLeftHandOrthogonalVector
     */
    fun left(): Vector2 {
        val temp = this.x
        val newX = this.y
        val newY = -temp
        return Vector2(newX, newY)
    }

    /**
     * Returns a unit [Vector2] of this [Vector2].
     *
     *
     * This method requires the length of this [Vector2] is not zero.
     * @return [Vector2]
     */
    val normalized: Vector2
        get() {
            var magnitude = this.magnitude
            if (magnitude <= Epsilon.E) return Vector2()
            magnitude = 1.0 / magnitude
            return Vector2(this.x * magnitude, this.y * magnitude)
        }

    /**
     * Converts this [Vector2] into a unit [Vector2] and returns
     * the magnitude before normalization.
     *
     *
     * This method requires the length of this [Vector2] is not zero.
     * @return double
     */
    fun normalize(): Double {
        val magnitude = Math.sqrt(this.x * this.x + this.y * this.y)
        if (magnitude <= Epsilon.E) return 0.0
        val m = 1.0 / magnitude
        this.x *= m
        this.y *= m
        //return 1.0 / m;
        return magnitude
    }

    /**
     * Returns the smallest angle between the given [Vector2]s.
     *
     *
     * Returns the angle in radians in the range - to .
     * @param vector the [Vector2]
     * *
     * @return angle in radians [-, ]
     */
    fun getAngleBetween(vector: Vector2): Double {
        val a = Math.atan2(vector.y, vector.x) - Math.atan2(this.y, this.x)
        if (a > Math.PI) return a - 2.0 * Math.PI
        if (a < -Math.PI) return a + 2.0 * Math.PI
        return a
    }

    companion object {
        /** A vector representing the x-axis; this vector should not be changed at runtime; used internally  */
        internal val X_AXIS = Vector2(1.0, 0.0)

        /** A vector representing the y-axis; this vector should not be changed at runtime; used internally  */
        internal val Y_AXIS = Vector2(0.0, 1.0)

        /**
         * Returns a new [Vector2] given the magnitude and direction.
         * @param magnitude the magnitude of the [Vector2]
         * *
         * @param direction the direction of the [Vector2] in radians
         * *
         * @return [Vector2]
         */
        fun create(magnitude: Double, direction: Double): Vector2 {
            val x = magnitude * Math.cos(direction)
            val y = magnitude * Math.sin(direction)
            return Vector2(x, y)
        }

        /**
         * The triple product of [Vector2]s is defined as:
         *
         * a x (b x c)
         *
         * However, this method performs the following triple product:
         *
         * (a x b) x c
         *
         * this can be simplified to:
         *
         * -a * (b  c) + b * (a  c)
         *
         * or:
         *
         * b * (a  c) - a * (b  c)
         *
         * @param a the a [Vector2] in the above equation
         * *
         * @param b the b [Vector2] in the above equation
         * *
         * @param c the c [Vector2] in the above equation
         * *
         * @return [Vector2]
         */
        fun tripleProduct(a: Vector2, b: Vector2, c: Vector2): Vector2 {
            // expanded version of above formula
            val r = Vector2()
            // perform a.dot(c)
            val ac = a.x * c.x + a.y * c.y
            // perform b.dot(c)
            val bc = b.x * c.x + b.y * c.y
            // perform b * a.dot(c) - a * b.dot(c)
            r.x = b.x * ac - a.x * bc
            r.y = b.y * ac - a.y * bc
            return r
        }
    }
}