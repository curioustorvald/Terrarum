/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.torvald.gdx.graphics

import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.VectorOperators
import kotlin.math.roundToInt

/**
 * A Cvec is kind of a Vector4f made compatible with LibGdx's Color class, with the intention of actually utilising
 * the JEP 338 VectorInstrinsics later, when the damned thing finally releases.
 *
 * -XX:+UseVectorApiIntrinsics --add-modules=jdk.incubator.vector
 *
 * Before then, the code will be identical to LibGdx's.
 */
inline class Cvec constructor(val vec: FloatVector) {

    constructor(floatArray: FloatArray) : this(FloatVector.fromArray(SPECIES, floatArray, 0))
    constructor(r: Float, g: Float, b: Float, a: Float) : this(FloatVector.fromValues(SPECIES, r, g, b, a))
    constructor(scalar: Float) : this(FloatVector.fromValues(SPECIES, scalar, scalar, scalar, scalar))
    constructor() : this(FloatVector.zero(SPECIES))
    constructor(rgba8888: Int) : this(FloatVector.fromValues(SPECIES,
            ((rgba8888 ushr 24) and 0xff) / 255f,
            ((rgba8888 ushr 16) and 0xff) / 255f,
            ((rgba8888 ushr 8) and 0xff) / 255f,
            (rgba8888 and 0xff) / 255f
    ))

    companion object {
        private val EPSILON = 1f / 1024f
        private val SPECIES = FloatVector.SPECIES_128
    }

    //fun cpy(): Cvec = Cvec(this.vec)

    // not using shorthand names to prevent confusion with builtin functions

    fun multiply(other: FloatVector) = Cvec(vec.mul(other))
    infix operator fun times(other: Cvec) = multiply(other.vec)
    //infix operator fun times(scalar: Float) = Cvec(vec.mul(scalar))

    fun subtract(other: FloatVector) = Cvec(vec.sub(other))
    infix operator fun minus(other: Cvec) = subtract(other.vec)
    //infix operator fun minus(scalar: Float) = Cvec(vec.sub(scalar))

    fun addition(other: FloatVector) = Cvec(vec.add(other))
    infix operator fun plus(other: Cvec) = addition(other.vec)
    //infix operator fun plus(scalar: Float) = Cvec(vec.add(scalar))

    fun maximum(other: FloatVector): Cvec = Cvec(vec.max(other))
    infix fun max(other: Cvec): Cvec = maximum(other.vec)

    fun lerp(target: Cvec, t: Float): Cvec {
        return Cvec(t) * (target - this)
    }

    /**
     * true if at least one element in the vector is not zero.
     */
    fun nonZero(): Boolean = vec.reduceLanes(VectorOperators.MUL) != 0f //vec.toArray().fold(false) { acc, fl -> acc or (fl.absoluteValue >= EPSILON) }

    fun toRGBA8888(): Int {
        var acc = 0
        for (i in 0..3)
            acc += vec.lane(i).coerceIn(0f, 1f).times(255f).roundToInt().shl(8 * (3 - i))

        return acc
    }

    /*override fun equals(other: Any?): Boolean {
        return this.vec.equal((other as Cvec).vec).allTrue()
    }*/

    fun toGdxColor() = com.badlogic.gdx.graphics.Color(
            vec.lane(0),
            vec.lane(1),
            vec.lane(2),
            vec.lane(3)
    )
}
