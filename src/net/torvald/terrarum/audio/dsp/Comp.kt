package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.audio.fullscaleToDecibels
import net.torvald.terrarum.sqr
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created by minjaesong on 2024-12-24.
 */
class Comp(
    thresholdDB: Float,
    ratio0: Float,
    /** Knee is defined as the multiplier of the threshold. For example: if threshold is -12dB and kneeRatio is 0.5, the knee will be -24dB to -6dB */
    kneeDB: Float
): TerrarumAudioFilter(), DspCompressor {

    var threshold = thresholdDB
    var knee = kneeDB
    var ratio = ratio0
        set(value) {
            field = value
            r = value.toDouble()
            makeupGain = (1.0 - 1.0 / ratio) * (-threshold) * 0.5
            makeupGainLinear = decibelsToFullscale(makeupGain)

            if (ratio <= 1.0)
                bypass = true
            else
                bypass = false
        }

    private val t = threshold.toDouble()
    private var r = ratio.toDouble()
    private val k = knee.toDouble()

    private var makeupGain = (1.0 - 1.0 / ratio) * (-threshold) * 0.5
    private var makeupGainLinear = decibelsToFullscale(makeupGain)

    private var internalGainKnob = 1.0
    private var internalGainKnobWithoutGain = 1.0
    private var outputGainKnob = 1.0
    private var outputGainKnobWithoutGain = 1.0 // used for debug view only
    private val gainKnobWeight = 0.99 // outputGainKnob is updated per sample (48000 times per second)


    private fun calcGainMinusM(sampleL: Float, sampleR: Float, t: Double, r: Double, k: Double): Double {
        // https://www.desmos.com/calculator/o3uwcy3bzj

        val x = fullscaleToDecibels(maxOf(sampleL.absoluteValue, sampleR.absoluteValue).toDouble())

        val rx = r*x
        val rt = r*t
        val krt = k*r*t
        val halfk = k/2

        val fx = 1.0
        val gx = (rx+t-x)/(rt)
        val K = (((r-1)*(k-2*t).sqr())/(8*krt))+1
        val hx = (((r-1)*x*(k-2*t+x))/(2*krt))+K

        return if (x < t-halfk)
            fx
        else if (1 > x && x > t+halfk)
            gx
        else if (t-halfk <= x && x <= t+halfk)
            hx
        else
            1/r
    }


    override val downForce = arrayOf(1.0f, 1.0f)

    private var maxReduction = 1.0

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        downForce.fill(1.0f)
        maxReduction = 10000.0

        val outL = outbuf[0]
        val innL = inbuf[0]
        val outR = outbuf[1]
        val innR = inbuf[1]

        for (i in innL.indices) {
            // do the compression
            val sampleL = innL[i]
            val sampleR = innR[i]

            internalGainKnobWithoutGain = calcGainMinusM(sampleL, sampleR, t, r, k)
            internalGainKnob = internalGainKnobWithoutGain * makeupGainLinear
            outputGainKnobWithoutGain = FastMath.interpolateLinear(gainKnobWeight, internalGainKnobWithoutGain, outputGainKnobWithoutGain)
            outputGainKnob = FastMath.interpolateLinear(gainKnobWeight, internalGainKnob, outputGainKnob)

            outL[i] = (sampleL * outputGainKnob).toFloat()
            outR[i] = (sampleR * outputGainKnob).toFloat()

            // calculate the downforce
            maxReduction = minOf(maxReduction, outputGainKnobWithoutGain)

            downForce[0] = maxReduction.toFloat()
            downForce[1] = maxReduction.toFloat()
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
//        val reductionDB = fullscaleToDecibels(maxReduction)
//        val perc = (reductionDB / (-0.5*makeupGain)).toFloat().coerceIn(0f, 1f)
//        batch.color = COL_METER_GRAD2_YELLOW
//        Toolkit.fillArea(batch, x.toFloat() + STRIP_W, y.toFloat(), -STRIP_W * perc, 14f)
//        batch.color = COL_METER_GRAD_YELLOW
//        Toolkit.fillArea(batch, x.toFloat() + STRIP_W, y+14f, -STRIP_W * perc, 2f)

        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "P:${threshold.absoluteValue.toInt()}/${knee.absoluteValue.toInt()}", x+3f, y+1f)
        App.fontSmallNumbers.draw(batch, "R:${ratio.absoluteValue.times(100).roundToInt().div(100f)}:1", x+3f, y+17f)
    }

    override val debugViewHeight: Int = 32

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        if (other is Comp) {
            this.threshold = other.threshold
            this.knee = other.knee
            this.ratio = other.ratio
        }
    }
}

