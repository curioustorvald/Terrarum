package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.audio.TerrarumAudioMixerTrack

class Gain(var gain: Float): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until TerrarumAudioMixerTrack.BUFFER_SIZE / 4) {
            outbuf[0][i] = inbuf[0][i] * gain
            outbuf[1][i] = inbuf[1][i] * gain
        }
    }
}