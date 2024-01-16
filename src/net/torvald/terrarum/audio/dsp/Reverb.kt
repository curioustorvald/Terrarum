package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import kotlin.math.roundToInt

class Reverb(val delayMS: Float = 36f, var feedback: Float = 0.92f, var lowpass: Float = 1200f): TerrarumAudioFilter() {

    private val highpass = 80f

    private var delay = (TerrarumAudioMixerTrack.SAMPLING_RATEF * delayMS / 1000f).roundToInt()
    private val bufSize = delay + 2

    private val buf = Array(2) { FloatArray(bufSize) }

    private fun unshift(sample: Float, buf: FloatArray) {
        for (i in bufSize - 1 downTo 1) {
            buf[i] = buf[i - 1]
        }
        buf[0] = sample
    }

    private val out0 = FloatArray(2)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val RCLo: Float = 1f / (lowpass * FastMath.TWO_PI)
        val RCHi: Float = 1f / (highpass * FastMath.TWO_PI)
        val dt: Float = 1f / TerrarumAudioMixerTrack.SAMPLING_RATEF
        val alphaLo = dt / (RCLo + dt)
        val alphaHi = RCHi / (RCHi + dt)

        for (ch in outbuf.indices) {
            for (i in 0 until App.audioBufferSize) {
                val inn = inbuf[ch][i]

                // reverb
                val rev = buf[ch][delay - 1]
                val out = inn - rev * feedback

                // fill lpbuf
                val lp0 = buf[ch][0]
                val lp = lp0 + alphaLo * (out - lp0)
                unshift(lp, buf[ch])

                outbuf[ch][i] = out
            }
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
    }
}