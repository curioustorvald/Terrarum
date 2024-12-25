package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.audio.fullscaleToDecibels
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-12-24.
 */
class Comp(
    val thresholdDB: Float,
    val ratio: Float,
    /** Knee is defined as the multiplier of the threshold. For example: if threshold is -12dB and kneeRatio is 0.5, the knee will be -24dB to -6dB */
    val kneeRaio: Float
): TerrarumAudioFilter(), DspCompressor {

    private val thresholdLinear = decibelsToFullscale(thresholdDB.toDouble())
    private val kneeLinear = thresholdLinear * kneeRaio//decibelsToFullscale(kneeDB.toDouble())
    private val makeupGain = (1.0 - 1.0 / ratio) * (-thresholdDB)
    private val makeupGainLinear = decibelsToFullscale(makeupGain)

    private var internalGainKnob = 1.0
    private var internalGainKnobWithoutGain = 1.0
    private var outputGainKnob = 1.0
    private var outputGainKnobWithoutGain = 1.0 // used for debug view only
    private val gainKnobWeight = 0.93 // outputGainKnob is updated per sample (48000 times per second)


    private fun calcGainMinusM(sampleL: Float, sampleR: Float, thresholdLinear: Double, ratio: Float, kneeLinear: Double): Double {
        // https://www.desmos.com/calculator/p3wufeoioi

        val x = maxOf(sampleL.absoluteValue, sampleR.absoluteValue).toDouble()
        val t = thresholdLinear
        val k = kneeLinear
        val r = ratio.toDouble()
//        val M = (r-1) / r

        val rx = r*x
        val rt = r*t
        val rtt = r*t*t
        val kk = k*k
        val kkr = k*k*r
        val krt = k*r*t
        val kr = k*r
        val kt = k*t
        val kx = k*x
        val tt = t*t
        val tx = t*x
        val xx = x*x
        val halfk = k/2

        val fx = 1.0
        val gx = (rx-r+t-x)/(rt-r)
        val K = (kkr-kk+4*krt-8*kr+4*kt+4*rtt-4*tt)/(8*krt-8*kr)
        val hx = ((r-1)*(kx-2*tx+xx))/(2*krt-2*kr)+K


        return if (x < t-halfk)
            fx
        else if (1 > x && x > t+halfk)
            gx
        else if (t-halfk <= x && x <= t+halfk)
            hx
        else
            (1/r)
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

            internalGainKnobWithoutGain = calcGainMinusM(sampleL, sampleR, thresholdLinear, ratio, kneeLinear)
            internalGainKnob = internalGainKnobWithoutGain * makeupGainLinear
            outputGainKnobWithoutGain = FastMath.interpolateLinear(gainKnobWeight, internalGainKnobWithoutGain, outputGainKnobWithoutGain)
            outputGainKnob = FastMath.interpolateLinear(gainKnobWeight, internalGainKnob, outputGainKnob)

            outL[i] = (sampleL * internalGainKnob).toFloat()
            outR[i] = (sampleR * internalGainKnob).toFloat()

            // calculate the downforce
            maxReduction = minOf(maxReduction, outputGainKnobWithoutGain)

            downForce[0] = maxReduction.toFloat()
            downForce[1] = maxReduction.toFloat()
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        val reductionDB = fullscaleToDecibels(maxReduction)
        val perc = (reductionDB / (-0.5*makeupGain)).toFloat().coerceIn(0f, 1f)
        batch.color = COL_METER_GRAD2_YELLOW
        Toolkit.fillArea(batch, x.toFloat() + STRIP_W, y.toFloat(), -STRIP_W * perc, 14f)
        batch.color = COL_METER_GRAD_YELLOW
        Toolkit.fillArea(batch, x.toFloat() + STRIP_W, y+14f, -STRIP_W * perc, 2f)

        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "C:${reductionDB.absoluteValue.times(100).roundToInt().div(100f)}", x+3f, y+1f)
    }

    override val debugViewHeight: Int = 16

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        TODO()
    }

    private fun Double.unNaN(d: Double): Double {
        return if (this.isNaN()) d else this
    }
}

