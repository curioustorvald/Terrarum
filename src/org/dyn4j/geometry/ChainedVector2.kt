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
 * Kotlin translated and modified code Copyright (c) 2016 Torvald aka skyhi14.
 */
package org.dyn4j.geometry

import org.dyn4j.Epsilon

/**
 * This class represents a vector or point in 2D space.
 *
 *
 * The operations [ChainedVector2.setMagnitude], [ChainedVector2.getNormalized],
 * [ChainedVector2.project], and [ChainedVector2.normalize] require the [ChainedVector2]
 * to be non-zero in length.
 *
 *
 * Some methods also return the vector to facilitate chaining.  For example:
 *
 * Vector a = new Vector();
 * a.zero().plus(1, 2).times(2);
 *
 * In this Kotlin code, you can use regular operations like + - * /.
 *
 * |operator   |function    |
 * |-----------|------------|
 * |a + b      |a.plus(b)   | equivalent of plusAssign
 * |a - b      |a.minus(b)  | equivalent of minusAssign
 * |a * b      |a.times(b)  | equivalent of timesAssign
 * |a / b      |a.div(b)    | equivalent of divAssign
 * |a dot b    |a.dot(b)    | equivalent of dotAssign
 * |a cross b  |a.cross(b)  | equivalent of crossAssign
 * |!a         |negate(a)   |
 * |a rotate th|a.rotate(th)| equivalent of rotateAssign
 *
 * @author William Bittle
 * *
 * @version 3.1.11
 * *
 * @since 1.0.0
 */
class ChainedVector2 {

    /** The magnitude of the x component of this [ChainedVector2]  */
    var x: Double = 0.toDouble()

    /** The magnitude of the y component of this [ChainedVector2]  */
    var y: Double = 0.toDouble()

    /** Default constructor.  */
    constructor() {
    }

    /**
     * Copy constructor.
     * @param vector the [ChainedVector2] to copy from
     */
    constructor(vector: ChainedVector2) {
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
     * Optional constructor.
     * @param vector non-chainable Vector2
     */
    constructor(vector: Vector2) {
        this.x = vector.x
        this.y = vector.y
    }

    /**
     * Creates a [ChainedVector2] from the first point to the second point.
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
     * Creates a [ChainedVector2] from the first point to the second point.
     * @param p1 the first point
     * *
     * @param p2 the second point
     */
    constructor(p1: ChainedVector2, p2: ChainedVector2) {
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
     * Returns a copy of this [ChainedVector2].
     * @return [ChainedVector2]
     */
    fun copy(): ChainedVector2 {
        return ChainedVector2(this.x, this.y)
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
    fun distance(point: ChainedVector2): Double {
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
    fun distanceSquared(point: ChainedVector2): Double {
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
        if (obj is ChainedVector2) {
            return this.x == obj.x && this.y == obj.y
        }
        return false
    }

    /**
     * Returns true if the x and y components of this [ChainedVector2]
     * are the same as the given [ChainedVector2].
     * @param vector the [ChainedVector2] to compare to
     * *
     * @return boolean
     */
    fun equals(vector: ChainedVector2?): Boolean {
        if (vector == null) return false
        if (this === vector) {
            return true
        }
        else {
            return this.x == vector.x && this.y == vector.y
        }
    }

    /**
     * Returns true if the x and y components of this [ChainedVector2]
     * are the same as the given x and y components.
     * @param x the x coordinate of the [ChainedVector2] to compare to
     * *
     * @param y the y coordinate of the [ChainedVector2] to compare to
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
     * Sets this [ChainedVector2] to the given [ChainedVector2].
     * @param vector the [ChainedVector2] to set this [ChainedVector2] to
     * *
     * @return [ChainedVector2] this vector
     */
    fun set(vector: ChainedVector2): ChainedVector2 {
        this.x = vector.x
        this.y = vector.y
        return this
    }

    /**
     * Sets this [ChainedVector2] to the given [ChainedVector2].
     * @param x the x component of the [ChainedVector2] to set this [ChainedVector2] to
     * *
     * @param y the y component of the [ChainedVector2] to set this [ChainedVector2] to
     * *
     * @return [ChainedVector2] this vector
     */
    fun set(x: Double, y: Double): ChainedVector2 {
        this.x = x
        this.y = y
        return this
    }

    /**
     * Returns the x component of this [ChainedVector2].
     * @return [ChainedVector2]
     */
    val xComponent: ChainedVector2
        get() = ChainedVector2(this.x, 0.0)

    /**
     * Returns the y component of this [ChainedVector2].
     * @return [ChainedVector2]
     */
    val yComponent: ChainedVector2
        get() = ChainedVector2(0.0, this.y)

    /**
     * Returns the magnitude of this [ChainedVector2].
     * @return double
     */
    // the magnitude is just the pathagorean theorem
    val magnitude: Double
        get() = Math.sqrt(this.x * this.x + this.y * this.y)

    /**
     * Returns the magnitude of this [ChainedVector2] squared.
     * @return double
     */
    val magnitudeSquared: Double
        get() = this.x * this.x + this.y * this.y

    /**
     * Sets the magnitude of the [ChainedVector2].
     * @param magnitude the magnitude
     * *
     * @return [ChainedVector2] this vector
     */
    fun setMagnitude(magnitude: Double): ChainedVector2 {
        // check the given magnitude
        if (Math.abs(magnitude) <= Epsilon.E) {
            this.x = 0.0
            this.y = 0.0
            return this
        }
        // is this vector a zero vector?
        if (this.isZero) {
            return this
        }
        // get the magnitude
        var mag = Math.sqrt(this.x * this.x + this.y * this.y)
        // normalize and multiply by the new magnitude
        mag = magnitude / mag
        this.x *= mag
        this.y *= mag
        return this
    }

    /**
     * Returns the direction of this [ChainedVector2]
     * as an angle in radians.
     * @return double angle in radians [-, ]
     */
    val direction: Double
        get() = Math.atan2(this.y, this.x)

    /**
     * Sets the direction of this [ChainedVector2].
     * @param angle angle in radians
     * *
     * @return [ChainedVector2] this vector
     */
    fun setDirection(angle: Double): ChainedVector2 {
        //double magnitude = Math.hypot(this.x, this.y);
        val magnitude = Math.sqrt(this.x * this.x + this.y * this.y)
        this.x = magnitude * Math.cos(angle)
        this.y = magnitude * Math.sin(angle)
        return this
    }

    /**
     * Adds the given [ChainedVector2] to this [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2] this vector
     */
    fun plus(vector: ChainedVector2): ChainedVector2 {
        this.x += vector.x
        this.y += vector.y
        return this
    }

    /**
     * Adds the given [ChainedVector2] to this [ChainedVector2].
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return [ChainedVector2] this vector
     */
    fun plus(x: Double, y: Double): ChainedVector2 {
        this.x += x
        this.y += y
        return this
    }

    /**
     * Adds this [ChainedVector2] and the given [ChainedVector2] returning
     * a new [ChainedVector2] containing the result.
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun sum(vector: ChainedVector2): ChainedVector2 {
        return ChainedVector2(this.x + vector.x, this.y + vector.y)
    }

    /**
     * Adds this [ChainedVector2] and the given [ChainedVector2] returning
     * a new [ChainedVector2] containing the result.
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun sum(x: Double, y: Double): ChainedVector2 {
        return ChainedVector2(this.x + x, this.y + y)
    }

    /**
     * Subtracts the given [ChainedVector2] from this [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2] this vector
     */
    fun minus(vector: ChainedVector2): ChainedVector2 {
        this.x -= vector.x
        this.y -= vector.y
        return this
    }

    /**
     * Subtracts the given [ChainedVector2] from this [ChainedVector2].
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return [ChainedVector2] this vector
     */
    fun minus(x: Double, y: Double): ChainedVector2 {
        this.x -= x
        this.y -= y
        return this
    }

    /**
     * Subtracts the given [ChainedVector2] from this [ChainedVector2] returning
     * a new [ChainedVector2] containing the result.
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun difference(vector: ChainedVector2): ChainedVector2 {
        return ChainedVector2(this.x - vector.x, this.y - vector.y)
    }

    /**
     * Subtracts the given [ChainedVector2] from this [ChainedVector2] returning
     * a new [ChainedVector2] containing the result.
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun difference(x: Double, y: Double): ChainedVector2 {
        return ChainedVector2(this.x - x, this.y - y)
    }

    /**
     * Creates a [ChainedVector2] from this [ChainedVector2] to the given [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun to(vector: ChainedVector2): ChainedVector2 {
        return ChainedVector2(vector.x - this.x, vector.y - this.y)
    }

    /**
     * Creates a [ChainedVector2] from this [ChainedVector2] to the given [ChainedVector2].
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun to(x: Double, y: Double): ChainedVector2 {
        return ChainedVector2(x - this.x, y - this.y)
    }

    /**
     * Multiplies this [ChainedVector2] by the given scalar.
     * @param scalar the scalar
     * *
     * @return [ChainedVector2] this vector
     */
    fun times(scalar: Double): ChainedVector2 {
        this.x *= scalar
        this.y *= scalar
        return this
    }

    /**
     * Multiplies this [ChainedVector2] by the given scalar.
     * @param scalar the scalar
     * *
     * @return [ChainedVector2] this vector
     */
    fun div(scalar: Double): ChainedVector2 {
        this.x /= scalar
        this.y /= scalar
        return this
    }

    /**
     * Multiplies this [ChainedVector2] by the given scalar returning
     * a new [ChainedVector2] containing the result.
     * @param scalar the scalar
     * *
     * @return [ChainedVector2]
     */
    fun product(scalar: Double): ChainedVector2 {
        return ChainedVector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Returns the dot product of the given [ChainedVector2]
     * and this [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return double
     */
    fun dot(vector: ChainedVector2): Double {
        return this.x * vector.x + this.y * vector.y
    }

    /**
     * Returns the dot product of the given [ChainedVector2]
     * and this [ChainedVector2].
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return double
     */
    fun dot(x: Double, y: Double): Double {
        return this.x * x + this.y * y
    }

    /**
     * Returns the cross product of the this [ChainedVector2] and the given [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return double
     */
    fun cross(vector: ChainedVector2): Double {
        return this.x * vector.y - this.y * vector.x
    }

    /**
     * Returns the cross product of the this [ChainedVector2] and the given [ChainedVector2].
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return double
     */
    fun cross(x: Double, y: Double): Double {
        return this.x * y - this.y * x
    }

    /**
     * Returns the cross product of this [ChainedVector2] and the z value of the right [ChainedVector2].
     * @param z the z component of the [ChainedVector2]
     * *
     * @return [ChainedVector2]
     */
    fun cross(z: Double): ChainedVector2 {
        return ChainedVector2(-1.0 * this.y * z, this.x * z)
    }

    /**
     * Returns true if the given [ChainedVector2] is orthogonal (perpendicular)
     * to this [ChainedVector2].
     *
     *
     * If the dot product of this vector and the given vector is
     * zero then we know that they are perpendicular
     * @param vector the [ChainedVector2]
     * *
     * @return boolean
     */
    fun isOrthogonal(vector: ChainedVector2): Boolean {
        return if (Math.abs(this.x * vector.x + this.y * vector.y) <= Epsilon.E) true else false
    }

    /**
     * Returns true if the given [ChainedVector2] is orthogonal (perpendicular)
     * to this [ChainedVector2].
     *
     *
     * If the dot product of this vector and the given vector is
     * zero then we know that they are perpendicular
     * @param x the x component of the [ChainedVector2]
     * *
     * @param y the y component of the [ChainedVector2]
     * *
     * @return boolean
     */
    fun isOrthogonal(x: Double, y: Double): Boolean {
        return if (Math.abs(this.x * x + this.y * y) <= Epsilon.E) true else false
    }

    /**
     * Returns true if this [ChainedVector2] is the zero [ChainedVector2].
     * @return boolean
     */
    val isZero: Boolean
        get() = Math.abs(this.x) <= Epsilon.E && Math.abs(this.y) <= Epsilon.E

    /**
     * Negates this [ChainedVector2].
     * @return [ChainedVector2] this vector
     */
    fun not() = negate()

    /**
     * Negates this [ChainedVector2].
     * @return [ChainedVector2] this vector
     */
    fun negate(): ChainedVector2 {
        this.x *= -1.0
        this.y *= -1.0
        return this
    }

    /**
     * Returns a [ChainedVector2] which is the negative of this [ChainedVector2].
     * @return [ChainedVector2]
     */
    val negative: ChainedVector2
        get() = ChainedVector2(-this.x, -this.y)

    /**
     * Sets the [ChainedVector2] to the zero [ChainedVector2]
     * @return [ChainedVector2] this vector
     */
    fun zero(): ChainedVector2 {
        this.x = 0.0
        this.y = 0.0
        return this
    }

    /**
     * Rotates about the origin.
     * @param theta the rotation angle in radians
     * *
     * @return [ChainedVector2] this vector
     */
    fun rotate(theta: Double): ChainedVector2 {
        val cos = Math.cos(theta)
        val sin = Math.sin(theta)
        val x = this.x
        val y = this.y
        this.x = x * cos - y * sin
        this.y = x * sin + y * cos
        return this
    }

    /**
     * Rotates the [ChainedVector2] about the given coordinates.
     * @param theta the rotation angle in radians
     * *
     * @param x the x coordinate to rotate about
     * *
     * @param y the y coordinate to rotate about
     * *
     * @return [ChainedVector2] this vector
     */
    fun rotate(theta: Double, x: Double, y: Double): ChainedVector2 {
        this.x -= x
        this.y -= y
        this.rotate(theta)
        this.x += x
        this.y += y
        return this
    }

    /**
     * Rotates the [ChainedVector2] about the given point.
     * @param theta the rotation angle in radians
     * *
     * @param point the point to rotate about
     * *
     * @return [ChainedVector2] this vector
     */
    fun rotate(theta: Double, point: ChainedVector2): ChainedVector2 {
        return this.rotate(theta, point.x, point.y)
    }

    /**
     * Projects this [ChainedVector2] onto the given [ChainedVector2].
     * @param vector the [ChainedVector2]
     * *
     * @return [ChainedVector2] the projected [ChainedVector2]
     */
    fun project(vector: ChainedVector2): ChainedVector2 {
        val dotProd = this.dot(vector)
        var denominator = vector.dot(vector)
        if (denominator <= Epsilon.E) return ChainedVector2()
        denominator = dotProd / denominator
        return ChainedVector2(denominator * vector.x, denominator * vector.y)
    }

    /**
     * Returns the right-handed normal of this vector.
     * @return [ChainedVector2] the right hand orthogonal [ChainedVector2]
     */
    val rightHandOrthogonalVector: ChainedVector2
        get() = ChainedVector2(-this.y, this.x)

    /**
     * Sets this vector to the right-handed normal of this vector.
     * @return [ChainedVector2] this vector
     * *
     * @see .getRightHandOrthogonalVector
     */
    fun right(): ChainedVector2 {
        val temp = this.x
        this.x = -this.y
        this.y = temp
        return this
    }

    /**
     * Returns the left-handed normal of this vector.
     * @return [ChainedVector2] the left hand orthogonal [ChainedVector2]
     */
    val leftHandOrthogonalVector: ChainedVector2
        get() = ChainedVector2(this.y, -this.x)

    /**
     * Sets this vector to the left-handed normal of this vector.
     * @return [ChainedVector2] this vector
     * *
     * @see .getLeftHandOrthogonalVector
     */
    fun left(): ChainedVector2 {
        val temp = this.x
        this.x = this.y
        this.y = -temp
        return this
    }

    /**
     * Returns a unit [ChainedVector2] of this [ChainedVector2].
     *
     *
     * This method requires the length of this [ChainedVector2] is not zero.
     * @return [ChainedVector2]
     */
    val normalized: ChainedVector2
        get() {
            var magnitude = this.magnitude
            if (magnitude <= Epsilon.E) return ChainedVector2()
            magnitude = 1.0 / magnitude
            return ChainedVector2(this.x * magnitude, this.y * magnitude)
        }

    /**
     * Converts this [ChainedVector2] into a unit [ChainedVector2] and returns
     * the magnitude before normalization.
     *
     *
     * This method requires the length of this [ChainedVector2] is not zero.
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
     * Returns the smallest angle between the given [ChainedVector2]s.
     *
     *
     * Returns the angle in radians in the range - to .
     * @param vector the [ChainedVector2]
     * *
     * @return angle in radians [-, ]
     */
    fun getAngleBetween(vector: ChainedVector2): Double {
        val a = Math.atan2(vector.y, vector.x) - Math.atan2(this.y, this.x)
        if (a > Math.PI) return a - 2.0 * Math.PI
        if (a < -Math.PI) return a + 2.0 * Math.PI
        return a
    }

    fun toVector(): Vector2 {
        return Vector2(x, y)
    }

    companion object {
        /** A vector representing the x-axis; this vector should not be changed at runtime; used internally  */
        internal val X_AXIS = ChainedVector2(1.0, 0.0)

        /** A vector representing the y-axis; this vector should not be changed at runtime; used internally  */
        internal val Y_AXIS = ChainedVector2(0.0, 1.0)

        /**
         * Returns a new [ChainedVector2] given the magnitude and direction.
         * @param magnitude the magnitude of the [ChainedVector2]
         * *
         * @param direction the direction of the [ChainedVector2] in radians
         * *
         * @return [ChainedVector2]
         */
        fun create(magnitude: Double, direction: Double): ChainedVector2 {
            val x = magnitude * Math.cos(direction)
            val y = magnitude * Math.sin(direction)
            return ChainedVector2(x, y)
        }

        /**
         * The triple product of [ChainedVector2]s is defined as:
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
         * @param a the a [ChainedVector2] in the above equation
         * *
         * @param b the b [ChainedVector2] in the above equation
         * *
         * @param c the c [ChainedVector2] in the above equation
         * *
         * @return [ChainedVector2]
         */
        fun tripleProduct(a: ChainedVector2, b: ChainedVector2, c: ChainedVector2): ChainedVector2 {
            // expanded version of above formula
            val r = ChainedVector2()
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