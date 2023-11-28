package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import kotlin.math.roundToInt

class Scope : TerrarumAudioFilter() {
    val backbufL = Array((4096f / TerrarumAudioMixerTrack.BUFFER_SIZE * 4).roundToInt().coerceAtLeast(1)) { FloatArray(
        TerrarumAudioMixerTrack.BUFFER_SIZE / 4) }
    val backbufR = Array((4096f / TerrarumAudioMixerTrack.BUFFER_SIZE * 4).roundToInt().coerceAtLeast(1)) { FloatArray(
        TerrarumAudioMixerTrack.BUFFER_SIZE / 4) }

    private val sqrt2p = 0.7071067811865475

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        // shift buffer
        for (i in backbufL.lastIndex downTo 1) {
            backbufL[i] = backbufL[i - 1]
            backbufR[i] = backbufR[i - 1]
        }
        backbufL[0] = FloatArray(TerrarumAudioMixerTrack.BUFFER_SIZE / 4)
        backbufR[0] = FloatArray(TerrarumAudioMixerTrack.BUFFER_SIZE / 4)

        // plot dots
        for (i in 0 until TerrarumAudioMixerTrack.BUFFER_SIZE /4) {
            val y0 = inbuf[0][i] * 0.7
            val x0 = -inbuf[1][i] * 0.7 // rotate the domain by -90 deg

            val x = (+x0*sqrt2p -y0*sqrt2p) * 1.414
            val y = (-x0*sqrt2p -y0*sqrt2p) * 1.414 // further rotate by -45 deg then flip along the y axis

            backbufL[0][i] = x.toFloat()
            backbufR[0][i] = y.toFloat()
        }

        // copy samples over
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }
}