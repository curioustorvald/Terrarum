package net.torvald.terrarum.worlddrawer

import jdk.incubator.vector.FloatVector
import kotlin.math.roundToInt


/**
 * Get and compile your OpenJDK-Panama for linux/mac/windows at https://jdk.java.net/panama/
 * Or use pre-built ones in https://github.com/minjaesong/openjdk13-vectorintrinsic/tree/master
 *
 * NOTE: Panama's new vectors are all immutable.
 * NOTE2: Class inlining vastly improves the performance
 *
 * Created by minjaesong on 2019-05-20.
 */
inline class Cvec constructor(val vec: FloatVector) {

    /*
Test log

I ran with following jvm args:
    -XX:TypeProfileLevel=121 -XX:+UseVectorApiIntrinsics --add-modules=jdk.incubator.vector
and it's slower than the legacy.

This time I removed the TypeProfileLevel part. It now runs slightly faster than the legacy?
    -XX:+UseVectorApiIntrinsics --add-modules=jdk.incubator.vector
    
<<append from here>>
     */

    constructor(floatArray: FloatArray) : this(FloatVector.fromArray(SPECIES, floatArray, 0))
    constructor(r: Float, g: Float, b: Float, a: Float) : this(FloatVector.scalars(SPECIES, r, g, b, a))
    constructor(scalar: Float) : this(FloatVector.scalars(SPECIES, scalar, scalar, scalar, scalar))
    constructor() : this(FloatVector.zero(SPECIES))

    companion object {
        private val EPSILON = 1f / 512f
        private val SPECIES = FloatVector.SPECIES_128
    }

    //fun cpy(): Cvec = Cvec(this.vec)

    fun multiply(other: FloatVector) = Cvec(vec.mul(other))
    infix operator fun times(other: Cvec) = multiply(other.vec)
    infix operator fun times(scalar: Float) = Cvec(vec.mul(scalar))

    fun subtract(other: FloatVector) = Cvec(vec.sub(other))
    infix operator fun minus(other: Cvec) = subtract(other.vec)
    infix operator fun minus(scalar: Float) = Cvec(vec.sub(scalar))

    fun addition(other: FloatVector) = Cvec(vec.add(other))
    infix operator fun plus(other: Cvec) = addition(other.vec)
    infix operator fun plus(scalar: Float) = Cvec(vec.add(scalar))

    fun maximum(other: FloatVector): Cvec = Cvec(vec.max(other))
    infix fun max(other: Cvec): Cvec = maximum(other.vec)

    /**
     * true if at least one element in the vector is not zero.
     */
    fun nonZero(): Boolean = vec.mulLanes() != 0f

    fun toRGBA8888(): Int {
        var acc = 0
        for (i in 0..3)
            acc += vec.lane(i).coerceIn(0f, 1f).times(255f).roundToInt().shl(8 * (3 - i))

        return acc
    }

    /*override fun equals(other: Any?): Boolean {
        return this.vec.equal((other as Cvec).vec).allTrue()
    }*/
}

