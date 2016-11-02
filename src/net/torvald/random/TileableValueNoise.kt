package net.torvald.random

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.round
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.fmod
import java.util.*

/**
 * Generate value noise that is always "tileably looped" every x in loopSize.
 *
 * @param width: power of 2's are recommended.
 * Created by minjaesong on 16-10-28.
 */
class TileableValueNoise(
        val octaves: Int, val persistency: Float, val width: Int) {

    private val noiseData = Array<Float>(width + 1, { 0f })
    private var noiseGenerated = false

    fun generate(seed: Long) {
        val rng = HQRNG(seed)

        // initialise
        Arrays.fill(noiseData, 0f)


        for (i in 1..octaves) {
            // octave 1 samples four points
            val samples = 4 * i
            val amp = FastMath.pow(persistency, (i - 1).toFloat())

            var pointThis = 0f
            var pointNext = rng.nextBipolarFloat()
            var pointLoop = pointThis


            for (x in 0..width) {
                val thisSampleStart: Int = // 0-256 -> 0-4 -> 0-256(qnt)
                        (x / width.toFloat() * samples).floorInt() * (width / samples)
                val nextSampleStart: Int =
                        (x / width.toFloat() * samples).floorInt().plus(1) * (width / samples)
                val stepWithinWindow: Int = x % (width / samples)
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