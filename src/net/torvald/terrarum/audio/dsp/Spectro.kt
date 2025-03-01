package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.sqrt
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.*

/**
 * Created by minjaesong on 2023-12-21.
 */
class Spectro(var gain: Float = 1f) : TerrarumAudioFilter() {
    private val FFTSIZE = 1024
    private val inBuf = Array(2) { FloatArray(FFTSIZE) }

    private fun sin2(x: Double) = sin(x).pow(2)

    private val chsum = ComplexArray(FloatArray(2*FFTSIZE))
    private val fftOut = ComplexArray(FloatArray(2*FFTSIZE))
    private val fftWin = FloatArray(FFTSIZE) { sin2(PI * it / FFTSIZE).toFloat() } // hann

    private val a0 = 0.21557895
    private val a1 = 0.41663158
    private val a2 = 0.277263158
    private val a3 = 0.083578947
    private val a4 = 0.006947368
    private val FT = PI / FFTSIZE
//    private val fftWin = FloatArray(FFTSIZE) { (a0 - a1*cos(2*it*FT) + a2*cos(4*it*FT) - a3*cos(6*it*FT) + a4*cos(8*it*FT)).toFloat() } // flat-top

    private val sqrt2p = 0.7071067811865475

    private val oldFFTmagn = DoubleArray(FFTSIZE / 2) { 0.0 }

    private fun push(samples: FloatArray, buf: FloatArray) {
        if (samples.size >= FFTSIZE) {
            // overwrite
            System.arraycopy(samples, samples.size - buf.size, buf, 0, buf.size)
        }
        else {
            // shift samples
            System.arraycopy(buf, samples.size, buf, 0, buf.size - samples.size)
            // write to the buf
            System.arraycopy(samples, 0, buf, buf.size - samples.size, samples.size)
        }
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        // create (L+R)/2 array
        push(inbuf[0], inBuf[0])
        push(inbuf[1], inBuf[1])
        for (i in 0 until FFTSIZE) {
            chsum.reim[2*i] = ((inBuf[0][i] + inBuf[1][i]) / 2f) * fftWin[i] * gain
        }

        // do fft
        FFT.fftInto(chsum, fftOut)

        // copy samples over
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }

//    private val spectroPlotCol = Color(0xdf6fa0_aa.toInt())
    private val spectroPlotCol = Color(0x61b3df_aa)

    private val lowlim = -60.0

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        // spectrometer
        batch.color = spectroPlotCol
        for (bin in 0 until FFTSIZE / 2) {
            val freqL = (SAMPLING_RATED / FFTSIZE) * bin
            val freqR = (SAMPLING_RATED / FFTSIZE) * (bin + 1)
            val magn0 = fftOut.reim[2 * bin].absoluteValue / FFTSIZE * freqR.sqrt() // apply slope
            val magn = FastMath.interpolateLinear(BasicDebugInfoWindow.FFT_SMOOTHING_FACTOR, magn0, oldFFTmagn[bin])
            val magnLog = fullscaleToDecibels(magn)

            if (magnLog >= lowlim) {
                val xL = linToLogPerc(freqL, 24.0, 24000.0).coerceIn(0.0, 1.0) * STRIP_W
                val xR = linToLogPerc(freqR, 24.0, 24000.0).coerceIn(0.0, 1.0) * STRIP_W
                val w = (xR - xL)
                val h = (magnLog - lowlim) / lowlim * STRIP_W
                Toolkit.fillArea(batch, x + xL.toFloat(), y + STRIP_W.toFloat(), w.toFloat(), h.toFloat())
            }

            oldFFTmagn[bin] = magn
        }
    }

    override val debugViewHeight = STRIP_W

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        if (other is Spectro) {
            this.gain = other.gain
        }
    }
}

/**
 * Created by minjaesong on 2023-11-20.
 */
class Vecto(var gain: Float = 1f) : TerrarumAudioFilter() {
    var backbufL = Array((6144f / App.audioBufferSize).roundToInt().coerceAtLeast(1)) {
        FloatArray(App.audioBufferSize)
    }
    var backbufR = Array((6144f / App.audioBufferSize).roundToInt().coerceAtLeast(1)) {
        FloatArray(App.audioBufferSize)
    }

    private val sqrt2p = 0.7071067811865475

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        // shift buffer
        for (i in backbufL.lastIndex downTo 1) {
            backbufL[i] = backbufL[i - 1]
            backbufR[i] = backbufR[i - 1]
        }
        backbufL[0] = FloatArray(App.audioBufferSize)
        backbufR[0] = FloatArray(App.audioBufferSize)

        // plot dots
        for (i in 0 until App.audioBufferSize) {
            val y0 = +inbuf[0][i] * gain
            val x0 = -inbuf[1][i] * gain// rotate the domain by -90 deg

            val x = (+x0*sqrt2p -y0*sqrt2p) * 1.4142
            val y = (-x0*sqrt2p -y0*sqrt2p) * 1.4142 // further rotate by -45 deg then flip along the y axis

            backbufL[0][i] = x.toFloat()
            backbufR[0][i] = y.toFloat()
        }

        // copy samples over
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }

    private val halfStripW = STRIP_W / 2

    private val scopePlotCol = Color(0xdf6fa0_33.toInt())

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        // vectorscope
        batch.color = scopePlotCol
        val xxs = backbufR
        val yys = backbufL
        for (t in xxs.lastIndex downTo 0) {
            val xs = xxs[t]
            val ys = yys[t]
            for (i in xs.indices.reversed()) {
                val px = xs[i] * halfStripW + halfStripW
                val py = ys[i] * halfStripW + halfStripW
                Toolkit.fillArea(batch, x + px, y + py, 1f, 1f)
            }
        }

        // TODO correlation meter
    }

    override val debugViewHeight = STRIP_W

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        if (other is Vecto) {
            this.gain = other.gain
        }
    }
}