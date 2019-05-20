package net.torvald.terrarum.worlddrawer

import kotlin.math.roundToInt

//import jdk.incubator.vector.FloatVector


/**
 * Get your panama JDK for linux/mac/windows at https://jdk.java.net/panama/
 *
 * Created by minjaesong on 2019-05-20.
 */
class Cvec(val vec: FloatArray) {

    constructor(r: Float, g: Float, b: Float, a: Float) : this(floatArrayOf(r, g, b, a))
    constructor(scalar: Float) : this(FloatArray(4) { scalar })
    constructor() : this(FloatArray(4) { 0f })

    private val epsilon = 1f / 512f

    fun cpy(): Cvec = Cvec(this.vec)

    fun setTo(scalar: Float) = setTo(FloatArray(4) { scalar })
    fun setTo(other: Cvec) = setTo(other.vec)
    fun setTo(other: FloatArray): Cvec {
        for (i in 0..3) {
            this.vec[i] = other[i]
        }
        return this
    }

    infix fun mul(scalar: Float): Cvec = mul(FloatArray(4) { scalar })
    infix fun mul(other: Cvec) = mul(other.vec)

    fun mul(other: FloatArray): Cvec {
        for (i in 0..3) {
            this.vec[i] *= other[i]
        }
        return this
    }

    fun max(other: Cvec): Cvec = max(other.vec)

    fun max(other: FloatArray): Cvec {
        for (i in 0..3) {
            this.vec[i] = if (this.vec[i] >= other[i]) this.vec[i] else other[i]
        }
        return this
    }

    /**
     * true if at least one element in the vector is not zero.
     */
    fun nonZero(): Boolean {
        var oracc = 0 // set to 1 if the vector element is zero
        for (i in 0..3) {
            if (vec[i] in 0f..epsilon)
                oracc = oracc or 1
        }

        return (oracc != 0)
    }

    fun toRGBA8888(): Int {
        var acc = 0
        for (i in 0..3)
            acc += vec[i].coerceIn(0f, 1f).times(255f).roundToInt().shl(8 * (3 - i))

        return acc
    }

}


//hg clone http://hg.openjdk.java.net/panama/dev/