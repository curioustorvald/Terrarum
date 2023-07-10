package net.torvald.random

import com.jme3.math.FastMath
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.gameworld.fmod
import java.util.*

/**
 * Generate value noise that is always "tileably looped" every x in loopSize.
 *
 * @param width: power of 2's are recommended.
 * Created by minjaesong on 2016-10-28.
 */
class TileableValueNoise(
        val octaves: Int, val persistency: Float, val width: Int, val initSamples: Int = 4) {

    init {
        // FIXME wow, such primitive!
        if (!FastMath.isPowerOfTwo(width)) throw Error("width is not power of two!")
        if (!FastMath.isPowerOfTwo(initSamples)) throw Error("initSamples is not power of two!")
        if (initSamples < 2) throw Error("initSamples must be equal to or greater than 2, and power of two!")
    }

    private val noiseData = Array<Float>(width + 1, { 0f })
    private var noiseGenerated = false

    fun generate(seed: Long) {
        val rng = HQRNG(seed)

        // initialise
        Arrays.fill(noiseData, 0f)

        val octavesIntStream = Array(octaves, { 1.shl(it) }) // array of (1, 2, 4, 8, ...)


        octaveLoop@ for (octcnt in 0..octaves - 1) { // 1, 2, 3, 4, ...
            val i = octavesIntStream[octcnt] // 1, 2, 4, 8, ...
            // octave 1 samples four points
            val samples = initSamples * i
            val amp = FastMath.pow(persistency, octcnt.toFloat()) // 1/1, 1/2, 1/3, 1/4, ...
            var pointThis = 0f
            var pointNext = rng.nextBipolarFloat()
            var pointLoop = pointThis

            try {
                for (x in 0..width) {
                    val thisSampleStart: Int = // 0-256 -> 0-4 -> 0-256(qnt)
                            (x / width.toFloat() * samples).floorToInt() * (width / samples)
                    val nextSampleStart: Int =
                            (x / width.toFloat() * samples).floorToInt().plus(1) * (width / samples)
                    val stepWithinWindow: Int = x % (nextSampleStart - thisSampleStart)
                    val windowScale: Float = stepWithinWindow.toFloat() / (width / samples)

                    // next pair of points
                    if (stepWithinWindow == 0 && x > 0) {
                        pointThis = pointNext
                        pointNext = if (nextSampleStart >= width) pointLoop else rng.nextBipolarFloat()
                    }


                    // additive mix
                    val noiseValue = FastMath.interpolateLinear(windowScale, pointThis, pointNext) * amp
                    noiseData[x] += noiseValue


                    /*println("x: $x\tstart: $thisSampleStart\tnext: $nextSampleStart\t" +
                        "window: $stepWithinWindow\t" +
                        "pThis: $pointThis\tpNext: $pointNext\tvalue: $noiseValue")*/
                }
            }
            catch (e: ArithmeticException) {
                //println("[TileableValueNoise] division by zero error occured, aborting further octave iteration.")
                break@octaveLoop
            } // division by zero; which means octave value was too high
        }

        for (x in 0..width - 1) {
            //println(noiseData[x])
        }

        noiseGenerated = true
    }

    operator fun get(x: Int): Float {
        if (!noiseGenerated) throw Error("Noise not generated; use 'generate(seed: Long)'")
        return noiseData[x fmod width]
    }

    private fun Random.nextBipolarFloat(): Float {
        val d = this.nextFloat()
        return d.times(2f).minus(1f)
    }
}