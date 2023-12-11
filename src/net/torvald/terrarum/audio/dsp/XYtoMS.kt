package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.audio.TerrarumAudioMixerTrack

object XYtoMS: TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) {
            val X = inbuf[0][i]
            val Y = inbuf[1][i]
            val M = (X + Y) / 2f
            val S = (X - Y) / 2f
            outbuf[0][i] = M
            outbuf[1][i] = S
        }
    }
}

object MStoXY: TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) {
            val M = inbuf[0][i]
            val S = inbuf[1][i]
            val X = M + S
            val Y = M - S
            outbuf[0][i] = X
            outbuf[1][i] = Y
        }
    }
}