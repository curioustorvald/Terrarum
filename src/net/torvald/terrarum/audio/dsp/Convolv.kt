package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.App.setDebugTime
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2_RED
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD_RED
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD_YELLOW
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.Toolkit
import java.io.File
import kotlin.math.roundToInt

/**
 * @param ir Binary file containing MONO IR (containing only two channels)
 * @param crossfeed The amount of channel crossfeeding to simulate the true stereo IR. Fullscale (0.0 - 1.0)
 * @param gain output gain. Fullscale (0.0 - 1.0)
 */
class Convolv(ir: File, val crossfeed: Float, gain: Float = 1f / 256f): TerrarumAudioFilter() {

    private val gain: Float = gain / (1f + crossfeed)

    val fftLen: Int
    private val convFFT: Array<ComplexArray>
    private val sumbuf: Array<ComplexArray>

    var processingSpeed = 1f; private set

    init {
        if (!ir.exists()) {
            throw IllegalArgumentException("Impulse Response file '${ir.path}' does not exist.")
        }

        val sampleCount = (ir.length().toInt() / 8)//.coerceAtMost(65536)
        fftLen = FastMath.nextPowerOfTwo(sampleCount)

        println("IR '${ir.path}' Sample Count = $sampleCount; FFT Length = $fftLen")

        val conv = Array(2) { FloatArray(fftLen) }
        sumbuf = Array(2) { ComplexArray(FloatArray(fftLen * 2)) }

        ir.inputStream().let {
            for (i in 0 until sampleCount) {
                val f1 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                val f2 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                conv[0][i] = f1
                conv[1][i] = f2
            }

            it.close()
        }

        // fourier-transform the 'conv'
        convFFT = Array(2) {
            FFT.fft(conv[it])
        }
    }

    private var realtime = (App.audioBufferSize / TerrarumAudioMixerTrack.SAMPLING_RATEF * 1000000000L)
    private val fftIn = ComplexArray(FloatArray(fftLen * 2))
    private val fftMult = ComplexArray(FloatArray(fftLen * 2))
    private val fftOutL = FloatArray(fftLen)
    private val fftOutR = FloatArray(fftLen)

    private fun convolve(x: ComplexArray, h: ComplexArray, output: FloatArray) {
        FFT.fftInto(x, fftIn)
        fftIn.mult(h, fftMult)
        FFT.ifftAndGetReal(fftMult, output)
    }

    /**
     * https://thewolfsound.com/fast-convolution-fft-based-overlap-add-overlap-save-partitioned/
     */
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val t1 = System.nanoTime()



        pushSum(gain, inbuf[0], inbuf[1], sumbuf)

        convolve(sumbuf[0], convFFT[0], fftOutL)
        convolve(sumbuf[1], convFFT[1], fftOutR)

        for (i in 0 until App.audioBufferSize) {
            outbuf[0][i] = fftOutL[fftLen - App.audioBufferSize + i]
            outbuf[1][i] = fftOutR[fftLen - App.audioBufferSize + i]
        }



        val ptime = System.nanoTime() - t1
        setDebugTime("audio.convolve", ptime)
        processingSpeed = realtime / ptime
    }


    private fun push(gain: Float, samples: FloatArray, buf: ComplexArray) {
        // shift numbers
        System.arraycopy(buf.reim, samples.size * 2, buf.reim, 0, buf.reim.size - samples.size * 2)
        // fill in the shifted area
        val baseI = buf.reim.size - samples.size * 2
        samples.forEachIndexed { index, fl ->
            buf.reim[baseI + index * 2 + 0] = fl * gain
            buf.reim[baseI + index * 2 + 1] = 0f
        }
    }

    private fun pushSum(gain: Float, sampleL: FloatArray, sampleR: FloatArray, sumbuf: Array<ComplexArray>) {
        // shift numbers
        System.arraycopy(sumbuf[0].reim, sampleL.size * 2, sumbuf[0].reim, 0, sumbuf[0].reim.size - sampleL.size * 2)
        System.arraycopy(sumbuf[1].reim, sampleL.size * 2, sumbuf[1].reim, 0, sumbuf[1].reim.size - sampleL.size * 2)
        // fill in the shifted area
        val baseI = sumbuf[0].reim.size - sampleL.size * 2
        for (index in sampleL.indices) {
            sumbuf[0].reim[baseI + index * 2 + 0] = (sampleL[index] * 1.000000f + sampleR[index] * crossfeed) * gain
            sumbuf[0].reim[baseI + index * 2 + 1] = 0f
            sumbuf[1].reim[baseI + index * 2 + 0] = (sampleL[index] * crossfeed + sampleR[index] * 1.000000f) * gain
            sumbuf[1].reim[baseI + index * 2 + 1] = 0f
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    // processing speed bar
        val w = processingSpeed
        val perc = w.coerceAtMost(2f) / 2f
        batch.color = if (w > 1.5f) COL_METER_GRAD2 else if (w > 1f) COL_METER_GRAD2_YELLOW else COL_METER_GRAD2_RED
        Toolkit.fillArea(batch, x.toFloat(), y.toFloat(), STRIP_W * perc, 14f)
        batch.color = if (w > 1.5f) COL_METER_GRAD else if (w > 1f) COL_METER_GRAD_YELLOW else COL_METER_GRAD_RED
        Toolkit.fillArea(batch, x.toFloat(), y+14f, STRIP_W * perc, 2f)

        // filter length bar
        val g = FastMath.intLog2(App.audioBufferSize)
        val perc2 = (FastMath.intLog2(fftLen).minus(g).toFloat() / (16f - g)).coerceIn(0f, 1f)
        batch.color = COL_METER_GRAD2
        Toolkit.fillArea(batch, x.toFloat(), y + 16f, STRIP_W * perc2, 14f)
        batch.color = COL_METER_GRAD
        Toolkit.fillArea(batch, x.toFloat(), y + 16f+14f, STRIP_W * perc2, 2f)

        // texts
        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "P:${processingSpeed.times(100).roundToInt().div(100f)}x", x+3f, y+1f)
        App.fontSmallNumbers.draw(batch, "L:${fftLen}", x+3f, y+17f)
    }

    override val debugViewHeight = 32

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
    }
}